/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.carbondata.core.metadata.schema.table;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.apache.carbondata.common.logging.LogService;
import org.apache.carbondata.common.logging.LogServiceFactory;
import org.apache.carbondata.core.constants.CarbonCommonConstants;
import org.apache.carbondata.core.metadata.AbsoluteTableIdentifier;
import org.apache.carbondata.core.metadata.CarbonTableIdentifier;

/**
 * Store the information about the table.
 * it stores the fact table as well as aggregate table present in the schema
 */
public class TableInfo implements Serializable, Writable {

  private static final LogService LOGGER =
      LogServiceFactory.getLogService(TableInfo.class.getName());

  /**
   * serialization version
   */
  private static final long serialVersionUID = -5034287968314105193L;

  /**
   * name of the database;
   */
  private String databaseName;

  /**
   * table name to group fact table and aggregate table
   */
  private String tableUniqueName;

  /**
   * fact table information
   */
  private TableSchema factTable;

  /**
   * last updated time to update the table if any changes
   */
  private long lastUpdatedTime;

  /**
   * metadata file path (check if it is really required )
   */
  private String metaDataFilepath;

  /**
   * store location
   */
  private String storePath;

  // this idenifier is a lazy field which will be created when it is used first time
  private AbsoluteTableIdentifier identifier;

  public TableInfo() {
  }

  /**
   * @return the factTable
   */
  public TableSchema getFactTable() {
    return factTable;
  }

  /**
   * @param factTable the factTable to set
   */
  public void setFactTable(TableSchema factTable) {
    this.factTable = factTable;
  }

  /**
   * @return the databaseName
   */
  public String getDatabaseName() {
    return databaseName;
  }

  /**
   * @param databaseName the databaseName to set
   */
  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

  /**
   * @return the tableUniqueName
   */
  public String getTableUniqueName() {
    return tableUniqueName;
  }

  /**
   * @param tableUniqueName the tableUniqueName to set
   */
  public void setTableUniqueName(String tableUniqueName) {
    this.tableUniqueName = tableUniqueName;
  }

  /**
   * @return the lastUpdatedTime
   */
  public long getLastUpdatedTime() {
    return lastUpdatedTime;
  }

  /**
   * @param lastUpdatedTime the lastUpdatedTime to set
   */
  public void setLastUpdatedTime(long lastUpdatedTime) {
    this.lastUpdatedTime = lastUpdatedTime;
  }

  /**
   * @return
   */
  public String getMetaDataFilepath() {
    return metaDataFilepath;
  }

  /**
   * @param metaDataFilepath
   */
  public void setMetaDataFilepath(String metaDataFilepath) {
    this.metaDataFilepath = metaDataFilepath;
  }

  public String getStorePath() {
    return storePath;
  }

  public void setStorePath(String storePath) {
    this.storePath = storePath;
  }

  /**
   * to generate the hash code
   */
  @Override public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((databaseName == null) ? 0 : databaseName.hashCode());
    result = prime * result + ((tableUniqueName == null) ? 0 : tableUniqueName.hashCode());
    return result;
  }

  /**
   * Overridden equals method
   */
  @Override public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof TableInfo)) {
      return false;
    }
    TableInfo other = (TableInfo) obj;
    if (databaseName == null) {
      if (other.databaseName != null) {
        return false;
      }
    } else if (!tableUniqueName.equals(other.tableUniqueName)) {
      return false;
    }

    if (tableUniqueName == null) {
      if (other.tableUniqueName != null) {
        return false;
      }
    } else if (!tableUniqueName.equals(other.tableUniqueName)) {
      return false;
    }
    return true;
  }

  /**
   * This method will return the table size. Default table block size will be considered
   * in case not specified by the user
   */
  int getTableBlockSizeInMB() {
    String tableBlockSize = null;
    // In case of old store there will not be any map for table properties so table properties
    // will be null
    Map<String, String> tableProperties = getFactTable().getTableProperties();
    if (null != tableProperties) {
      tableBlockSize = tableProperties.get(CarbonCommonConstants.TABLE_BLOCKSIZE);
    }
    if (null == tableBlockSize) {
      tableBlockSize = CarbonCommonConstants.BLOCK_SIZE_DEFAULT_VAL;
      LOGGER.info("Table block size not specified for " + getTableUniqueName()
          + ". Therefore considering the default value "
          + CarbonCommonConstants.BLOCK_SIZE_DEFAULT_VAL + " MB");
    }
    return Integer.parseInt(tableBlockSize);
  }

  @Override
  public void write(DataOutput out) throws IOException {
    out.writeUTF(databaseName);
    out.writeUTF(tableUniqueName);
    factTable.write(out);
    out.writeLong(lastUpdatedTime);
    out.writeUTF(metaDataFilepath);
    out.writeUTF(storePath);
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    this.databaseName = in.readUTF();
    this.tableUniqueName = in.readUTF();
    this.factTable = new TableSchema();
    this.factTable.readFields(in);
    this.lastUpdatedTime = in.readLong();
    this.metaDataFilepath = in.readUTF();
    this.storePath = in.readUTF();
  }

  public AbsoluteTableIdentifier getOrCreateAbsoluteTableIdentifier() {
    if (identifier == null) {
      CarbonTableIdentifier carbontableIdentifier =
          new CarbonTableIdentifier(databaseName, factTable.getTableName(), factTable.getTableId());
      identifier = new AbsoluteTableIdentifier(storePath, carbontableIdentifier);
    }
    return identifier;
  }

  public byte[] serialize() throws IOException {
    ByteArrayOutputStream bao = new ByteArrayOutputStream();
    this.write(new DataOutputStream(bao));
    return bao.toByteArray();
  }

  public static TableInfo deserialize(byte[] bytes) throws IOException {
    TableInfo tableInfo = new TableInfo();
    DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
    tableInfo.readFields(in);
    return tableInfo;
  }
}
