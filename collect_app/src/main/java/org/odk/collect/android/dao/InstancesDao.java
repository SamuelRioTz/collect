/*
 * Copyright 2017 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.odk.collect.android.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.odk.collect.android.application.Collect;
import org.odk.collect.android.instances.Instance;
import org.odk.collect.android.provider.InstanceProviderAPI.InstanceColumns;
import org.odk.collect.android.storage.StoragePathProvider;
import org.odk.collect.android.utilities.ApplicationConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to encapsulate all access to the {@link org.odk.collect.android.provider.InstanceProvider#DATABASE_NAME}
 * For more information about this pattern go to https://en.wikipedia.org/wiki/Data_access_object
 */
public class InstancesDao {

    public Cursor getSavedInstancesCursor(String sortOrder) {
        String selection = InstanceColumns.DELETED_DATE + " IS NULL ";

        return getInstancesCursor(null, selection, null, sortOrder);
    }

    public Cursor getInstancesCursorForFilePath(String path) {
        String selection = InstanceColumns.INSTANCE_FILE_PATH + "=?";
        String[] selectionArgs = {new StoragePathProvider().getRelativeInstancePath(path)};

        return getInstancesCursor(null, selection, selectionArgs, null);
    }

    public Cursor getAllCompletedUndeletedInstancesCursor() {
        String selection = InstanceColumns.DELETED_DATE + " IS NULL and ("
                + InstanceColumns.STATUS + "=? or "
                + InstanceColumns.STATUS + "=? or "
                + InstanceColumns.STATUS + "=?)";

        String[] selectionArgs = {Instance.STATUS_COMPLETE,
                Instance.STATUS_SUBMISSION_FAILED,
                Instance.STATUS_SUBMITTED};
        String sortOrder = InstanceColumns.DISPLAY_NAME + " ASC";

        return getInstancesCursor(null, selection, selectionArgs, sortOrder);
    }

    public Cursor getInstancesCursor(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return Collect.getInstance().getContentResolver().query(InstanceColumns.CONTENT_URI, projection, selection, selectionArgs, sortOrder);
    }

    public Uri saveInstance(ContentValues values) {
        return Collect.getInstance().getContentResolver().insert(InstanceColumns.CONTENT_URI, values);
    }

    public int updateInstance(ContentValues values, String where, String[] whereArgs) {
        return Collect.getInstance().getContentResolver().update(InstanceColumns.CONTENT_URI, values, where, whereArgs);
    }

    public void deleteInstancesFromInstanceFilePaths(List<String> instanceFilePaths) {
        int count = instanceFilePaths.size();
        int counter = 0;
        while (count > 0) {
            String[] selectionArgs = null;
            if (count > ApplicationConstants.SQLITE_MAX_VARIABLE_NUMBER) {
                selectionArgs = new String[
                        ApplicationConstants.SQLITE_MAX_VARIABLE_NUMBER];
            } else {
                selectionArgs = new String[count];
            }

            StringBuilder selection = new StringBuilder();
            selection.append(InstanceColumns.INSTANCE_FILE_PATH + " IN (");
            int j = 0;
            while (j < selectionArgs.length) {
                selectionArgs[j] = new StoragePathProvider().getRelativeInstancePath(instanceFilePaths.get(
                        counter * ApplicationConstants.SQLITE_MAX_VARIABLE_NUMBER + j));
                selection.append('?');

                if (j != selectionArgs.length - 1) {
                    selection.append(',');
                }
                j++;
            }
            counter++;
            count -= selectionArgs.length;
            selection.append(')');
            Collect.getInstance().getContentResolver()
                    .delete(InstanceColumns.CONTENT_URI,
                            selection.toString(), selectionArgs);

        }
    }

    /**
     * Returns all instances available through the cursor and closes the cursor.
     */
    public List<Instance> getInstancesFromCursor(Cursor cursor) {
        List<Instance> instances = new ArrayList<>();
        if (cursor != null) {
            try {
                cursor.moveToPosition(-1);
                while (cursor.moveToNext()) {
                    int displayNameColumnIndex = cursor.getColumnIndex(InstanceColumns.DISPLAY_NAME);
                    int submissionUriColumnIndex = cursor.getColumnIndex(InstanceColumns.SUBMISSION_URI);
                    int canEditWhenCompleteIndex = cursor.getColumnIndex(InstanceColumns.CAN_EDIT_WHEN_COMPLETE);
                    int instanceFilePathIndex = cursor.getColumnIndex(InstanceColumns.INSTANCE_FILE_PATH);
                    int jrFormIdColumnIndex = cursor.getColumnIndex(InstanceColumns.JR_FORM_ID);
                    int jrVersionColumnIndex = cursor.getColumnIndex(InstanceColumns.JR_VERSION);
                    int statusColumnIndex = cursor.getColumnIndex(InstanceColumns.STATUS);
                    int lastStatusChangeDateColumnIndex = cursor.getColumnIndex(InstanceColumns.LAST_STATUS_CHANGE_DATE);
                    int deletedDateColumnIndex = cursor.getColumnIndex(InstanceColumns.DELETED_DATE);
                    int geometryTypeColumnIndex = cursor.getColumnIndex(InstanceColumns.GEOMETRY_TYPE);
                    int geometryColumnIndex = cursor.getColumnIndex(InstanceColumns.GEOMETRY);

                    int databaseIdIndex = cursor.getColumnIndex(InstanceColumns._ID);

                    Instance instance = new Instance.Builder()
                            .displayName(cursor.getString(displayNameColumnIndex))
                            .submissionUri(cursor.getString(submissionUriColumnIndex))
                            .canEditWhenComplete(Boolean.valueOf(cursor.getString(canEditWhenCompleteIndex)))
                            .instanceFilePath(cursor.getString(instanceFilePathIndex))
                            .jrFormId(cursor.getString(jrFormIdColumnIndex))
                            .jrVersion(cursor.getString(jrVersionColumnIndex))
                            .status(cursor.getString(statusColumnIndex))
                            .lastStatusChangeDate(cursor.getLong(lastStatusChangeDateColumnIndex))
                            .deletedDate(cursor.isNull(deletedDateColumnIndex) ? null : cursor.getLong(deletedDateColumnIndex))
                            .geometryType(cursor.getString(geometryTypeColumnIndex))
                            .geometry(cursor.getString(geometryColumnIndex))
                            .id(cursor.getLong(databaseIdIndex))
                            .build();

                    instances.add(instance);
                }
            } finally {
                cursor.close();
            }
        }
        return instances;
    }

    /**
     * Returns the values of an instance as a ContentValues object for use with
     * {@link #saveInstance(ContentValues)} or {@link #updateInstance(ContentValues, String, String[])}
     * <p>
     * Does NOT include the database ID.
     */
    public ContentValues getValuesFromInstanceObject(Instance instance) {
        ContentValues values = new ContentValues();
        values.put(InstanceColumns.DISPLAY_NAME, instance.getDisplayName());
        values.put(InstanceColumns.SUBMISSION_URI, instance.getSubmissionUri());
        values.put(InstanceColumns.CAN_EDIT_WHEN_COMPLETE, Boolean.toString(instance.canEditWhenComplete()));
        values.put(InstanceColumns.INSTANCE_FILE_PATH, instance.getInstanceFilePath());
        values.put(InstanceColumns.JR_FORM_ID, instance.getJrFormId());
        values.put(InstanceColumns.JR_VERSION, instance.getJrVersion());
        values.put(InstanceColumns.STATUS, instance.getStatus());
        values.put(InstanceColumns.LAST_STATUS_CHANGE_DATE, instance.getLastStatusChangeDate());
        values.put(InstanceColumns.DELETED_DATE, instance.getDeletedDate());
        values.put(InstanceColumns.GEOMETRY, instance.getGeometry());
        values.put(InstanceColumns.GEOMETRY_TYPE, instance.getGeometryType());
        return values;
    }
}
