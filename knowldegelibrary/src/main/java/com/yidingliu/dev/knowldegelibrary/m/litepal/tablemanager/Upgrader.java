/*
 * yidingliu.com Inc. * Copyright (c) 2016 All Rights Reserved.
 */

package com.yidingliu.dev.knowldegelibrary.m.litepal.tablemanager;

import android.database.sqlite.SQLiteDatabase;

import com.yidingliu.dev.knowldegelibrary.m.litepal.crud.model.AssociationsInfo;
import com.yidingliu.dev.knowldegelibrary.m.litepal.tablemanager.model.ColumnModel;
import com.yidingliu.dev.knowldegelibrary.m.litepal.tablemanager.model.TableModel;
import com.yidingliu.dev.knowldegelibrary.m.litepal.util.Const;
import com.yidingliu.dev.knowldegelibrary.m.litepal.util.DBUtility;
import com.yidingliu.dev.knowldegelibrary.m.litepal.util.LogUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Upgrade the database. The first step is to remove the columns that can not
 * find the corresponding field in the model class. Then add the new added field
 * as new column into the table. At last it will check all the types of columns
 * to see which are changed.
 * 
 * @author Tony Green
 * @since 1.0
 */
public class Upgrader extends AssociationUpdater {
	/**
	 * Model class for table.
	 */
	protected TableModel mTableModel;

    /**
     * Model class for table from database.
     */
    protected TableModel mTableModelDB;

    /**
     * Indicates that column constraints has changed or not.
     */
    private boolean hasConstraintChanged;

	/**
	 * Analyzing the table model, them remove the dump columns and add new
	 * columns of a table.
	 */
	@Override
	protected void createOrUpgradeTable(SQLiteDatabase db, boolean force) {
		mDb = db;
		for (TableModel tableModel : getAllTableModels()) {
			mTableModel = tableModel;
            mTableModelDB = getTableModelFromDB(tableModel.getTableName());
			upgradeTable();
		}
	}

	/**
	 * Upgrade table actions. Include remove dump columns, add new columns and
	 * change column types. All the actions above will be done by the description
     * order.
	 */
	private void upgradeTable() {
        if (hasNewUniqueOrNotNullColumn()) {
            // Need to drop the table and create new one. Cause unique column can not be added, and null data can not be migrated.
            createOrUpgradeTable(mTableModel, mDb, true);
            // add foreign keys of the table.
            Collection<AssociationsInfo > associationsInfo = getAssociationInfo ( mTableModel.getClassName () );
            for (AssociationsInfo info : associationsInfo) {
                if (info.getAssociationType() == Const.Model.MANY_TO_ONE
                        || info.getAssociationType() == Const.Model.ONE_TO_ONE) {
                    if (info.getClassHoldsForeignKey().equalsIgnoreCase(mTableModel.getClassName())) {
                        String associatedTableName = DBUtility
                                .getTableNameByClassName ( info.getAssociatedClassName () );
                        addForeignKeyColumn(mTableModel.getTableName(), associatedTableName, mTableModel.getTableName(), mDb);
                    }
                }
            }
        } else {
            hasConstraintChanged = false;
            removeColumns(findColumnsToRemove());
            addColumns(findColumnsToAdd());
            changeColumnsType(findColumnTypesToChange());
            changeColumnsConstraints();
        }
	}

    /**
     * Check if the current model add or upgrade an unique or not null column.
     * @return True if has new unique or not null column. False otherwise.
     */
    private boolean hasNewUniqueOrNotNullColumn() {
        List<ColumnModel > columnModelList = mTableModel.getColumnModels ();
        for (ColumnModel columnModel : columnModelList) {
            ColumnModel columnModelDB = mTableModelDB.getColumnModelByName(columnModel.getColumnName());
            if (columnModel.isUnique()) {
                if (columnModelDB == null || !columnModelDB.isUnique()) {
                    return true;
                }
            }
            if (columnModelDB != null && !columnModel.isNullable() && columnModelDB.isNullable()) {
                return true;
            }
        }
        return false;
    }

	/**
	 * It will find the difference between class model and table model. If
	 * there's a field in the class without a corresponding column in the table,
	 * this field is a new added column. This method find all new added columns.
	 * 
	 * @return List with ColumnModel contains information of new columns.
	 */
	private List<ColumnModel> findColumnsToAdd() {
        List<ColumnModel> columnsToAdd = new ArrayList<ColumnModel>();
        for (ColumnModel columnModel : mTableModel.getColumnModels()) {
            String columnName = columnModel.getColumnName();
            if (!mTableModelDB.containsColumn(columnName)) {
                // add column action
                columnsToAdd.add(columnModel);
            }
        }
		return columnsToAdd;
	}

	/**
	 * This method helps find the difference between table model from class and
	 * table model from database. Database should always be synchronized with
	 * model class. If there're some fields are removed from class, the table
	 * model from database will be compared to find out which fields are
	 * removed. But there're still some exceptions. The columns named id or _id
	 * won't ever be removed. The foreign key column will be checked some where
	 * else, not from here.
	 * 
	 * @return A list with column names need to remove.
	 */
	private List<String> findColumnsToRemove() {
        String tableName = mTableModel.getTableName();
		List<String> removeColumns = new ArrayList<String>();
        List<ColumnModel> columnModelList = mTableModelDB.getColumnModels();
        for (ColumnModel columnModel : columnModelList) {
            String dbColumnName = columnModel.getColumnName();
            if (isNeedToRemove(dbColumnName)) {
                removeColumns.add(dbColumnName);
            }
        }
        LogUtil.d ( TAG, "remove columns from " + tableName + " >> " + removeColumns );
		return removeColumns;
	}

	/**
	 * It will check each class in the mapping list. Find their types for each
	 * field is changed or not by comparing with the types in table columns. If
	 * there's a column have same name as a field in class but with different
	 * type, then it's a type changed column.
	 *
	 * @return A list contains all ColumnModel which type are changed from database.
	 */
	private List<ColumnModel> findColumnTypesToChange() {
        List<ColumnModel> columnsToChangeType = new ArrayList<ColumnModel>();
        for (ColumnModel columnModelDB : mTableModelDB.getColumnModels()) {
            for (ColumnModel columnModel : mTableModel.getColumnModels()) {
                if (columnModelDB.getColumnName().equalsIgnoreCase(columnModel.getColumnName())) {
                    if (!columnModelDB.getColumnType().equalsIgnoreCase(columnModel.getColumnType())) {
                        // column type is changed
                        columnsToChangeType.add(columnModel);
                    }
                    if (!hasConstraintChanged) {
                        // for reducing loops, check column constraints change here.
                        LogUtil.d(TAG, "default value db is:" + columnModelDB.getDefaultValue() + ", default value is:" + columnModel.getDefaultValue());
                        if (columnModelDB.isNullable() != columnModel.isNullable() ||
                            !columnModelDB.getDefaultValue().equalsIgnoreCase(columnModel.getDefaultValue()) ||
                            (columnModelDB.isUnique() && !columnModel.isUnique())) { // unique constraint can not be added
                            hasConstraintChanged = true;
                        }
                    }
                }
            }
        }
		return columnsToChangeType;
	}

	/**
	 * Tell LitePal the column is need to remove or not. The column can be
	 * remove only on the condition that the following three rules are all
	 * passed. First the corresponding field for this column is removed in the
	 * class. Second this column is not an id column. Third this column is not a
	 * foreign key column.
	 * 
	 * @param columnName
	 *            The column name to judge
	 * @return Need to remove return true, otherwise return false.
	 */
	private boolean isNeedToRemove(String columnName) {
		return isRemovedFromClass(columnName) && !isIdColumn(columnName)
				&& !isForeignKeyColumn(mTableModel, columnName);
	}

	/**
	 * Read a column name from database, and judge the corresponding field in
	 * class is removed or not.
	 * 
	 * @param columnName
	 *            The column name to judge.
	 * @return If it's removed return true, or return false.
	 */
	private boolean isRemovedFromClass(String columnName) {
        return !mTableModel.containsColumn(columnName);
	}

	/**
	 * Generate a SQL for add new column into the existing table.
	 * 
	 * @param columnModel
	 *            Which contains column info.
	 * @return A SQL to add new column.
	 */
	private String generateAddColumnSQL(ColumnModel columnModel) {
		return generateAddColumnSQL(mTableModel.getTableName(), columnModel);
	}

	/**
	 * This method create a SQL array for the all new columns to add them into
	 * table.
	 * 
	 * @param columnModelList
	 *            List with ColumnModel to add new column.
	 * @return A SQL array contains add all new columns job.
	 */
	private String[] getAddColumnSQLs(List<ColumnModel> columnModelList) {
		List<String> sqls = new ArrayList<String>();
		for (ColumnModel columnModel : columnModelList) {
			sqls.add(generateAddColumnSQL(columnModel));
		}
		return sqls.toArray(new String[0]);
	}

    /**
     * When some fields are removed from class, the table should synchronize the
     * changes by removing the corresponding columns.
     *
     * @param removeColumnNames
     *            The column names that need to remove.
     */
    private void removeColumns(List<String> removeColumnNames) {
        LogUtil.d(TAG, "do addColumn");
        removeColumns(removeColumnNames, mTableModel.getTableName());
        for (String columnName : removeColumnNames) {
            mTableModelDB.removeColumnModelByName(columnName);
        }
    }

	/**
	 * When some fields are added into the class after last upgrade, the table
	 * should synchronize the changes by adding the corresponding columns.
	 * 
	 * @param columnModelList
	 *            List with ColumnModel to add new column.
	 */
	private void addColumns(List<ColumnModel> columnModelList) {
        LogUtil.d(TAG, "do addColumn");
		execute(getAddColumnSQLs(columnModelList), mDb);
        for (ColumnModel columnModel : columnModelList) {
            mTableModelDB.addColumnModel(columnModel);
        }
	}

	/**
	 * When some fields type are changed in class, the table should drop the
	 * before columns and create new columns with same name but new types.
	 * 
	 * @param columnModelList
	 *            List with ColumnModel to change column type.
	 */
	private void changeColumnsType(List<ColumnModel> columnModelList) {
        LogUtil.d(TAG, "do changeColumnsType");
        List<String> columnNames = new ArrayList<String>();
        if (columnModelList != null && !columnModelList.isEmpty()) {
            for (ColumnModel columnModel : columnModelList) {
                columnNames.add(columnModel.getColumnName());
            }
        }
		removeColumns(columnNames);
		addColumns(columnModelList);
	}

    /**
     * When fields annotation changed in class, table should change the corresponding constraints
     * make them sync to the fields annotation.
     */
    private void changeColumnsConstraints() {
        if (hasConstraintChanged) {
            LogUtil.d(TAG, "do changeColumnsConstraints");
            execute(getChangeColumnsConstraintsSQL(), mDb);
        }
    }

    /**
     * This method create a SQL array for the whole changing column constraints job.
     * @return A SQL array contains create temporary table, create new table, add foreign keys,
     *         migrate data and drop temporary table.
     */
    private String[] getChangeColumnsConstraintsSQL() {
        String alterToTempTableSQL = generateAlterToTempTableSQL(mTableModel.getTableName());
        String createNewTableSQL = generateCreateTableSQL(mTableModel);
        List<String> addForeignKeySQLs = generateAddForeignKeySQL();
        String dataMigrationSQL = generateDataMigrationSQL(mTableModelDB);
        String dropTempTableSQL = generateDropTempTableSQL(mTableModel.getTableName());
        List<String> sqls = new ArrayList<String>();
        sqls.add(alterToTempTableSQL);
        sqls.add(createNewTableSQL);
        sqls.addAll(addForeignKeySQLs);
        sqls.add(dataMigrationSQL);
        sqls.add(dropTempTableSQL);
        LogUtil.d(TAG, "generateChangeConstraintSQL >> ");
        for (String sql : sqls) {
            LogUtil.d(TAG, sql);
        }
        LogUtil.d(TAG, "<< generateChangeConstraintSQL");
        return sqls.toArray(new String[0]);
    }

    /**
     * Generate a SQL List for adding foreign keys. Changing constraints job should remain all the
     * existing columns including foreign keys. This method add origin foreign keys after creating
     * table.
     * @return A SQL List for adding foreign keys.
     */
    private List<String> generateAddForeignKeySQL() {
        List<String> addForeignKeySQLs = new ArrayList<String>();
        List<String> foreignKeyColumns = getForeignKeyColumns(mTableModel);
        for (String foreignKeyColumn : foreignKeyColumns) {
            if (!mTableModel.containsColumn(foreignKeyColumn)) {
                ColumnModel columnModel = new ColumnModel();
                columnModel.setColumnName(foreignKeyColumn);
                columnModel.setColumnType("integer");
                addForeignKeySQLs.add(generateAddColumnSQL(mTableModel.getTableName(), columnModel));
            }
        }
        return addForeignKeySQLs;
    }

}
