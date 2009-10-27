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
package org.apache.empire.db.codegen.types;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.empire.data.DataType;
import org.apache.empire.db.codegen.util.StringUtils;

public class Column {
    private static final Log log = LogFactory.getLog(Column.class);

    private String name;
	private int sqlType;
	private int colSize;		// max length if string, precision is numeric
	private int decimalDigits;	// max decimal digits allowed (real numbers)
	private boolean required;
	private String defaultValue;	
	private String fkTableName;	// Reference parent table if column if FK
	private boolean pkCol;
	private boolean lockingCol;	// 
	
	// Java transformation
	private String javaName;
	private DataType empireType;
	private String empireTypeString;
	private String originalJavaTypeString;
	private String javaTypeString;
	
	private boolean convertToBoolean;
	private String trueValue;
	private String falseValue;

	private boolean convertToEnum;
	
	public String getJavaName() {
		return javaName;
	}
	public void setJavaName(String javaName) {
		this.javaName = javaName;
	}
	public String getJavaTypeString() {
		return javaTypeString;
	}
	public String getEmpireTypeString() {
		return empireTypeString;
	}
	public DataType getEmpireType() {
		return empireType;
	}
	public Column(ResultSet rs) {
		this(rs, false, false);
	}
	public Column(ResultSet rs, boolean primaryKey) {
		this(rs, primaryKey, false);
	}
	public Column(ResultSet rs, boolean primaryKey, boolean foreignKey) {
		if (primaryKey && foreignKey) {
			throw new RuntimeException("Can't have a column that is both " +
					"a primary and a foreign key.");
		}
		
		try {
			if (!primaryKey && !foreignKey) {
				// Simple column
				this.name = rs.getString("COLUMN_NAME").toUpperCase();
				this.sqlType = rs.getInt("DATA_TYPE");
				this.empireType = this.deriveJavaInfo();
				this.colSize = rs.getInt("COLUMN_SIZE");
				this.decimalDigits = rs.getInt("DECIMAL_DIGITS");
				String nullable = rs.getString("IS_NULLABLE");
				if (nullable.equalsIgnoreCase("NO"))
					this.required = true;
				else
					this.required = false;
				this.defaultValue = rs.getString("COLUMN_DEF");
			}
			else if (primaryKey) {
				// Primary key column
				this.name = rs.getString("COLUMN_NAME").toUpperCase();
				this.sqlType = Types.INTEGER;
				this.empireType = DataType.AUTOINC;
				this.required = true;
				this.javaName = StringUtils.deriveAttributeName(this.name);
				this.javaTypeString = "Integer";
				this.pkCol = true;
			}
			else {
				// Foreign key column
				this.name = rs.getString("FKCOLUMN_NAME").toUpperCase();
				this.sqlType = Types.INTEGER;
				this.empireType = DataType.INTEGER;
				this.fkTableName = rs.getString("PKTABLE_NAME").toUpperCase();
				this.javaName = StringUtils.deriveAttributeName(this.name);
				this.javaTypeString = "Integer";
			}
			
			this.originalJavaTypeString = this.javaTypeString;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Column) {
			Column that = (Column)obj;
			if (this.name.equals(that.name)) {
				return true;
			}
		}
		return false;
	}
	
	public void convertToBoolean(String trueValue, String falseValue) {
		if (this.empireType != DataType.TEXT) {
			throw new RuntimeException(this.name + " must be a string type " +
					"to convert to boolean");
		}
		this.convertToBoolean = true;
		this.javaTypeString = "Boolean";
		this.trueValue = trueValue;
		this.falseValue = falseValue;
	}
	
	public boolean isForeignKey() {
		return (this.fkTableName != null);
	}
	public String getFkTableName() {
		return fkTableName;
	}
	public String getName() {
		return name.toUpperCase();
	}
	public int getSqlType() {
		return sqlType;
	}
	public int getColSize() {
		return colSize;
	}
	public int getDecimalDigits() {
		return decimalDigits;
	}
	public boolean isRequired() {
		return required;
	}
	public String getDefaultValue() {
		if (defaultValue == null) return "null";
		
		return "\"" + defaultValue + "\"";
	}
	public boolean isLockingCol() {
		return lockingCol;
	}
	public void setLockingCol(boolean lockingCol) {
		this.lockingCol = lockingCol;
	}
	public boolean isPkCol() {
		return this.pkCol;
	}
	public boolean isBoolean() {
		if (this.javaTypeString.equalsIgnoreCase("Boolean"))
			return true;
		return false;
	}
	public boolean isModifiable() {
		return (!this.isPkCol() && !this.isLockingCol());
	}
	
	private DataType deriveJavaInfo() {
		this.javaName = StringUtils.deriveAttributeName(this.name);
		DataType empireType = DataType.UNKNOWN;
		switch(this.sqlType) {
		case Types.INTEGER:
		case Types.SMALLINT:
		case Types.TINYINT:
		case Types.BIGINT:
			empireType = DataType.INTEGER;
			empireTypeString = "DataType.INTEGER";
			javaTypeString = "Long";
			break;
		case Types.VARCHAR:
			empireType = DataType.TEXT;
			empireTypeString = "DataType.TEXT";
			javaTypeString = "String";
			break;
		case Types.DATE:
			empireType = DataType.DATE;
			empireTypeString = "DataType.DATE";
			javaTypeString = "Date";
			break;
		case Types.TIMESTAMP:
		case Types.TIME:
			empireType = DataType.DATETIME;
			empireTypeString = "DataType.DATETIME";
			javaTypeString = "Date";
			break;
		case Types.CHAR:
			empireType = DataType.CHAR;
			empireTypeString = "DataType.CHAR";
			javaTypeString = "String";
			break;
		case Types.DOUBLE:
		case Types.FLOAT:
		case Types.REAL:
			empireType = DataType.DOUBLE;
			empireTypeString = "DataType.DOUBLE";
			javaTypeString = "Double";
			break;
		case Types.DECIMAL:
		case Types.NUMERIC:
			empireType = DataType.DECIMAL;
			empireTypeString = "DataType.DECIMAL";
			javaTypeString = "BigDecimal";
			break;
		case Types.BIT:
		case Types.BOOLEAN:
			empireType = DataType.BOOL;
			empireTypeString = "DataType.BOOL";
			javaTypeString = "Boolean";
			break;
		case Types.CLOB:
		case Types.LONGVARCHAR:
			empireType = DataType.CLOB;
			empireTypeString = "DataType.CLOB";
			javaTypeString = "String";
			break;
		case Types.BINARY:
		case Types.VARBINARY:
		case Types.LONGVARBINARY:
		case Types.BLOB:
			empireType = DataType.BLOB;
			empireTypeString = "DataType.BLOB";
			javaTypeString = "Byte[]";
			break;
		default:
			empireType = DataType.UNKNOWN;
			empireTypeString = "DataType.UNKNOWN";
			javaTypeString = "Byte[]";
			System.out.println("SQL column type " + this.sqlType + " not supported.");
		}
		
		return empireType;
	}
	public boolean isConvertToBoolean() {
		return convertToBoolean;
	}
	public String getTrueValue() {
		return trueValue;
	}
	public String getFalseValue() {
		return falseValue;
	}
	public String getOriginalJavaTypeString() {
		return originalJavaTypeString;
	}
	public boolean isConvertToEnum() {
		return convertToEnum;
	}
	public String getJavaAccessorName() {
		return StringUtils.deriveAccessorName(javaName, 
				this.javaTypeString.equalsIgnoreCase("boolean"));
	}
	public String getJavaMutatorName() {
		return StringUtils.deriveMutatorName(javaName);
	}
	
	public String getReturnExpression() {
		if (this.convertToBoolean) {
			StringBuilder sb = new StringBuilder(this.javaName);
			sb.append(".equalsIgnoreCase(\"");
			sb.append(this.trueValue).append("\")");
			return sb.toString();
		}
		else if (this.convertToEnum) {
			return this.javaTypeString + ".fromDbString(" +
			this.javaName + ")";
		}
		else {
			return this.javaName;
		}
	}
	public String getSetExpression() {
		if (this.convertToBoolean) {
			StringBuilder sb = new StringBuilder(this.javaName);
			sb.append(" ? \"").append(this.trueValue).append("\" ");
			sb.append(" : \"").append(this.falseValue).append("\")");
			return sb.toString();
		}
		else if (this.convertToEnum) {
			return this.javaName + ".toDbString()";
		}
		else {
			return this.javaName;
		}
	}
	public String getPopulateFkRecordMethodName() {
		StringBuilder sb = new StringBuilder(this.getName());
		StringUtils.replaceAll(sb, "_" + Table.getPkColName(),
				"");
		StringBuilder methodName = new StringBuilder("populate");
		methodName.append(StringUtils.javaClassName(sb.toString()));
		methodName.append("Record");
		return methodName.toString();
	}
}