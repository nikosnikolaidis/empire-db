/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.empire.db.postgresql;

import java.util.Iterator;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBDDLGenerator;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;

public class PostgreDDLGenerator extends DBDDLGenerator<DBDatabaseDriverPostgreSQL>
{
    public PostgreDDLGenerator(DBDatabaseDriverPostgreSQL driver)
    {
        super(driver);
        // set Oracle specific data types
        initDataTypes();
    }

    /**
     * sets Oracle specific data types
     * @param driver
     */
    private void initDataTypes()
    {   // Override data types
        DATATYPE_BOOLEAN = "BOOLEAN";
        DATATYPE_CLOB = "TEXT";
        DATATYPE_BLOB = "BYTEA";
    }

    @Override
    protected boolean appendColumnDataType(DataType type, double size, DBTableColumn c, StringBuilder sql)
    {
        switch (type)
        {
            case AUTOINC:
            { // Auto increment
                int bytes = Math.abs((int)size);
                if (bytes>= 8) {
                    sql.append("BIGSERIAL");
                } else {
                    sql.append("SERIAL");
                }
                //String seqName = createSequenceName(c);
                //sql.append(" DEFAULT nextval('"+seqName+"')");
                break;
            }
            case FLOAT:
            {   // only use double precision
                sql.append("DOUBLE PRECISION");
                break;
            }
            case BLOB:
                sql.append(DATATYPE_BLOB);
                break;
           default:
                // use default
                return super.appendColumnDataType(type, size, c, sql);
        }
        return true;
    }
    
    @Override
    protected void createDatabase(DBDatabase db, DBSQLScript script)
    {
        // Create all Sequences
        Iterator<DBTable> seqtabs = db.getTables().iterator();
        while (seqtabs.hasNext())
        {
            DBTable table = seqtabs.next();
            Iterator<DBColumn> cols = table.getColumns().iterator();
            while (cols.hasNext())
            {
                DBTableColumn c = (DBTableColumn) cols.next();
                if (c.getDataType() == DataType.AUTOINC)
                {
                    createSequence(db, c, script);
                }
            }
        }
        // default processing
        super.createDatabase(db, script);
    }

    /**
     * Appends the DDL-Script for creating a sequence to an SQL-Script<br/>
     * @param db the database to create
     * @param c the column for which to create the sequence
     * @param script the sql script to which to append the dll command(s)
     */
    protected void createSequence(DBDatabase db, DBTableColumn c, DBSQLScript script)
    {
    	String seqName = c.getSequenceName();
        // createSQL
        StringBuilder sql = new StringBuilder();
        sql.append("-- creating sequence for column ");
        sql.append(c.getFullName());
        sql.append(" --\r\n");
        sql.append("CREATE SEQUENCE ");
        db.appendQualifiedName(sql, seqName, detectQuoteName(seqName));
        
//        create sequence foo_id_seq;
//        select setval('foo_id_seq', (select max(id) from foo));

        sql.append(" INCREMENT BY 1 START WITH 1 MINVALUE 0");
        // executeDLL
        script.addStmt(sql);
    }

    @Override
    protected void appendColumnDesc(DBTableColumn c, boolean alter, StringBuilder sql)
    {
        // Append name
        c.addSQL(sql, DBExpr.CTX_NAME);
        // Alter or create
        if (alter) {
            sql.append(" TYPE ");
        } else {
            sql.append(" ");
        }
        // Unknown data type
        if (!appendColumnDataType(c.getDataType(), c.getSize(), c, sql))
            return;
        // Default Value
        if (driver.isDDLColumnDefaults() && !c.isAutoGenerated() && c.getDefaultValue()!=null)
        {   sql.append(" DEFAULT ");
            sql.append(driver.getValueString(c.getDefaultValue(), c.getDataType()));
        }
        // Nullable
        if (c.isRequired() ||  c.isAutoGenerated())
            sql.append(" NOT NULL");
    }
    
}

