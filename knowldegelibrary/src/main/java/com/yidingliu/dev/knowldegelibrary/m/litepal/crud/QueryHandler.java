/*
 * yidingliu.com Inc. * Copyright (c) 2016 All Rights Reserved.
 */

package com.yidingliu.dev.knowldegelibrary.m.litepal.crud;

import android.database.sqlite.SQLiteDatabase;

import com.yidingliu.dev.knowldegelibrary.m.litepal.util.BaseUtility;

import java.util.List;

/**
 * This is a component under DataSupport. It deals with query stuff as primary
 * task.
 * 
 * @author Tony Green
 * @since 1.1
 */
class QueryHandler extends DataHandler {

	/**
	 * Initialize {@link com.yidingliu.dev.knowldegelibrary.m.litepal.crud.DataHandler#mDatabase} for operating database. Do not
	 * allow to create instance of QueryHandler out of CRUD package.
	 * 
	 * @param db
	 *            The instance of SQLiteDatabase.
	 */
	QueryHandler(SQLiteDatabase db) {
		mDatabase = db;
	}

	/**
	 * The open interface for other classes in CRUD package to query a record
	 * based on id. If the result set is empty, gives null back.
	 * 
	 * @param modelClass
	 *            Which table to query and the object type to return.
	 * @param id
	 *            Which record to query.
	 * @param isEager
	 *            True to load the associated models, false not.
	 * @return An object with found data from database, or null.
	 */
	<T> T onFind(Class<T> modelClass, long id, boolean isEager) {
		List<T> dataList = query(modelClass, null, "id = ?", new String[] { String.valueOf(id) },
				null, null, null, null, getForeignKeyAssociations(modelClass.getName(), isEager));
		if (dataList.size() > 0) {
			return dataList.get(0);
		}
		return null;
	}

	/**
	 * The open interface for other classes in CRUD package to query the first
	 * record in a table. If the result set is empty, gives null back.
	 * 
	 * @param modelClass
	 *            Which table to query and the object type to return.
	 * @param isEager
	 *            True to load the associated models, false not.
	 * @return An object with data of first row, or null.
	 */
	<T> T onFindFirst(Class<T> modelClass, boolean isEager) {
		List<T> dataList = query(modelClass, null, null, null, null, null, "id", "1",
				getForeignKeyAssociations(modelClass.getName(), isEager));
		if (dataList.size() > 0) {
			return dataList.get(0);
		}
		return null;
	}

	/**
	 * The open interface for other classes in CRUD package to query the last
	 * record in a table. If the result set is empty, gives null back.
	 * 
	 * @param modelClass
	 *            Which table to query and the object type to return.
	 * @param isEager
	 *            True to load the associated models, false not.
	 * @return An object with data of last row, or null.
	 */
	<T> T onFindLast(Class<T> modelClass, boolean isEager) {
		List<T> dataList = query(modelClass, null, null, null, null, null, "id desc", "1",
				getForeignKeyAssociations(modelClass.getName(), isEager));
		if (dataList.size() > 0) {
			return dataList.get(0);
		}
		return null;
	}

	/**
	 * The open interface for other classes in CRUD package to query multiple
	 * records by an id array. Pass no ids means query all rows.
	 * 
	 * @param modelClass
	 *            Which table to query and the object type to return as a list.
	 * @param isEager
	 *            True to load the associated models, false not.
	 * @param ids
	 *            Which records to query. Or do not pass it to find all records.
	 * @return An object list with found data from database, or an empty list.
	 */
	<T> List<T> onFindAll(Class<T> modelClass, boolean isEager, long... ids) {
		List<T> dataList;
		if (isAffectAllLines(ids)) {
			dataList = query(modelClass, null, null, null, null, null, "id", null,
					getForeignKeyAssociations(modelClass.getName(), isEager));
		} else {
			dataList = query(modelClass, null, getWhereOfIdsWithOr(ids), null, null, null, "id",
					null, getForeignKeyAssociations(modelClass.getName(), isEager));
		}
		return dataList;
	}

	/**
	 * The open interface for other classes in CRUD package to query multiple
	 * records by parameters.
	 * 
	 * @param modelClass
	 *            Which table to query and the object type to return as a list.
	 * @param columns
	 *            A String array of which columns to return. Passing null will
	 *            return all columns.
	 * @param conditions
	 *            A filter declaring which rows to return, formatted as an SQL
	 *            WHERE clause. Passing null will return all rows.
	 * @param orderBy
	 *            How to order the rows, formatted as an SQL ORDER BY clause.
	 *            Passing null will use the default sort order, which may be
	 *            unordered.
	 * @param limit
	 *            Limits the number of rows returned by the query, formatted as
	 *            LIMIT clause.
	 * @param isEager
	 *            True to load the associated models, false not.
	 * @return An object list with found data from database, or an empty list.
	 */
	<T> List<T> onFind(Class<T> modelClass, String[] columns, String[] conditions, String orderBy,
			String limit, boolean isEager) {
		BaseUtility.checkConditionsCorrect ( conditions );
		return query(modelClass, columns, getWhereClause(conditions),
                getWhereArgs(conditions), null, null, orderBy, limit,
                getForeignKeyAssociations(modelClass.getName(), isEager));
	}

	/**
	 * The open interface for other classes in CRUD package to Count the
	 * records.
	 * 
	 * @param tableName
	 *            Which table to query from.
	 * @param conditions
	 *            A filter declaring which rows to return, formatted as an SQL
	 *            WHERE clause. Passing null will return all rows.
	 * @return Count of the specified table.
	 */
	int onCount(String tableName, String[] conditions) {
		return mathQuery(tableName, new String[] { "count(1)" }, conditions, int.class);
	}

	/**
	 * The open interface for other classes in CRUD package to calculate the
	 * average value on a given column.
	 * 
	 * @param tableName
	 *            Which table to query from.
	 * @param column
	 *            The based on column to calculate.
	 * @param conditions
	 *            A filter declaring which rows to return, formatted as an SQL
	 *            WHERE clause. Passing null will return all rows.
	 * @return The average value on a given column.
	 */
	double onAverage(String tableName, String column, String[] conditions) {
		return mathQuery(tableName, new String[] { "avg(" + column + ")" }, conditions,
				double.class);
	}

	/**
	 * The open interface for other classes in CRUD package to calculate the
	 * maximum value on a given column.
	 * 
	 * @param tableName
	 *            Which table to query from.
	 * @param column
	 *            The based on column to calculate.
	 * @param conditions
	 *            A filter declaring which rows to return, formatted as an SQL
	 *            WHERE clause. Passing null will return all rows.
	 * @param type
	 *            The type of the based on column.
	 * @return The maximum value on a given column.
	 */
	<T> T onMax(String tableName, String column, String[] conditions, Class<T> type) {
		return mathQuery(tableName, new String[] { "max(" + column + ")" }, conditions, type);
	}

	/**
	 * The open interface for other classes in CRUD package to calculate the
	 * minimum value on a given column.
	 * 
	 * @param tableName
	 *            Which table to query from.
	 * @param column
	 *            The based on column to calculate.
	 * @param conditions
	 *            A filter declaring which rows to return, formatted as an SQL
	 *            WHERE clause. Passing null will return all rows.
	 * @param type
	 *            The type of the based on column.
	 * @return The minimum value on a given column.
	 */
	<T> T onMin(String tableName, String column, String[] conditions, Class<T> type) {
		return mathQuery(tableName, new String[] { "min(" + column + ")" }, conditions, type);
	}

	/**
	 * The open interface for other classes in CRUD package to calculate the sum
	 * of values on a given column.
	 * 
	 * @param tableName
	 *            Which table to query from.
	 * @param column
	 *            The based on column to calculate.
	 * @param conditions
	 *            A filter declaring which rows to return, formatted as an SQL
	 *            WHERE clause. Passing null will return all rows.
	 * @param type
	 *            The type of the based on column.
	 * @return The sum value on a given column.
	 */
	<T> T onSum(String tableName, String column, String[] conditions, Class<T> type) {
		return mathQuery(tableName, new String[] { "sum(" + column + ")" }, conditions, type);
	}

}