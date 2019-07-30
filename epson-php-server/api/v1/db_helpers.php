<?php
include_once('../../conf/constant.php');

class DB {
  public static function get_database() {
    static $database = null;
    if ($database === null) {
        $database = new SQLite3(SQLITE_FILENAME);
        $database->busyTimeout(10000);
    }
    return $database;
  }

  public static function get_all_records($database, $tableName) {
    $statement = $database->prepare("SELECT * FROM " . $tableName);
    //$statement->bindParam(1, $data[0], SQLITE3_TEXT);

    $results = $statement->execute();

	  $records = [];

  	while ($record = $results->fetchArray(SQLITE3_ASSOC)) {
  	  array_push($records, $record);
  	}

  	return $records;
  }

  public static function get_records($database, $tableName, $whereFields) {
    $columns = [];
    $columnValues = [];
    $columnValuePlaceholders = [];
    foreach ($whereFields as $columnName => $columnValue) {
      array_push($columns, $columnName . '=:' . $columnName);
      array_push($columnValues, $columnValue);
      array_push($columnValuePlaceholders, ":" . $columnName);
    }

    $statement_string = "SELECT * FROM " . $tableName . " WHERE " . join(" AND ", $columns) . ";";

	$statement = $database->prepare($statement_string);

    $index = 0;

    foreach ($columnValues as $columnValue) {
        $statement->bindValue($columnValuePlaceholders[$index], $columnValue);

        $index += 1;
    }

    $results = $statement->execute();

    $records = [];

    while ($record = $results->fetchArray(SQLITE3_ASSOC)) {
  	array_push($records, $record);
    }

    return $records;
  }

  public static function get_records_where($database, $tableName, $whereStatement) {
    $statement_string = "SELECT * FROM " . $tableName . " WHERE " . $whereStatement . ";";

    error_log("test " . $statement_string);

    $statement = $database->prepare($statement_string);

    $results = $statement->execute();

    $records = [];

    while ($record = $results->fetchArray(SQLITE3_ASSOC)) {
      array_push($records, $record);
    }

    return $records;
  }

  public static function create_new_record($database, $tableName, $fields) {
    $columnNames = [];
    $columnValues = [];
	$columnValuePlaceholders = [];
    foreach ($fields as $columnName => $columnValue) {
      array_push($columnNames, $columnName);
      array_push($columnValues, $columnValue);
	  array_push($columnValuePlaceholders, ":" . $columnName);
    }

    $statement_string = "INSERT INTO " . $tableName . " (" . join(",",$columnNames) . ") VALUES (" . join(",",$columnValuePlaceholders) . ");";
    //error_log("statement = " . $statement_string);
    //error_log("compiled_statement = " . "INSERT INTO " . $tableName . " (" . join(",",$columnNames) . ") VALUES (" . join(",",$columnValues) . ");");
    $statement = $database->prepare($statement_string);
    //$statement->bindParam(1, $data[0], SQLITE3_TEXT);
    $index = 0;

    foreach ($columnValues as $columnValue) {
      /*
      if (is_bool($columnValue)) {
        //error_log('bool ' . $columnValue);
        $statement->bindValue($columnValuePlaceholders[$index], $columnValue);
      }
      elseif (is_int($columnValue)) {
        //error_log('int ' . $columnValue);
        $statement->bindValue($columnValuePlaceholders[$index], $columnValue, SQLITE3_INTEGER);
      }
      elseif (is_float($columnValue)) {
        //error_log('float ' . $columnValue);
        $statement->bindValue($columnValuePlaceholders[$index], $columnValue, SQLITE3_FLOAT);
      }
      else {
        //error_log('string ' . $columnValue);
        $statement->bindValue($columnValuePlaceholders[$index], $columnValue, SQLITE3_TEXT);
      }
      */
      $statement->bindValue($columnValuePlaceholders[$index], $columnValue);

  	$index += 1;
    }

    $results = $statement->execute();

    if (!$results) {
      //return $database->lastErrorMsg();
      return false;
    }

    //echo "Records created successfully";
    return true;
  }

  public static function update_record($database, $tableName, $whereFields, $updateFields) {
    $whereColumns = [];
    $whereColumnValues = [];
    $whereColumnValuePlaceholders = [];
    foreach ($whereFields as $whereColumnName => $whereColumnValue) {
      array_push($whereColumns, $whereColumnName . "=:" . $whereColumnName);
      array_push($whereColumnValues, $whereColumnValue);
      array_push($whereColumnValuePlaceholders, ":" . $whereColumnName);
    }

    $updateColumns = [];
    $updateColumnValues = [];
    $updateColumnValuePlaceholders = [];
    foreach ($updateFields as $updateColumnName => $updateColumnValue) {
      array_push($updateColumns, $updateColumnName . "=:" . $updateColumnName);
      array_push($updateColumnValues, $updateColumnValue);
      array_push($updateColumnValuePlaceholders, ":" . $updateColumnName);
    }

    $statement_string = "UPDATE " . $tableName . " SET " . join(",",$updateColumns) . " WHERE " . join(" AND ",$whereColumns) . ";";

    $statement = $database->prepare($statement_string);

    $index = 0;
    foreach ($whereColumnValues as $whereColumnValue) {
      $statement->bindValue($whereColumnValuePlaceholders[$index], $whereColumnValue);

      $index += 1;
    }

    $index = 0;
    foreach ($updateColumnValues as $updateColumnValue) {
      $statement->bindValue($updateColumnValuePlaceholders[$index], $updateColumnValue);

      $index += 1;
    }

    $results = $statement->execute();

    if (!$results) {
      //return $database->lastErrorMsg();
      return false;
    }

    //echo "Records created successfully";
    return true;
  }

  public static function delete_all_records($database, $tableName) {
    $sql = "DELETE FROM " . $tableName . ";";

    $returnValue = $database->exec($sql);
    if (!$returnValue) {
      //return database->lastErrorMsg();
      return false;
    }

    //echo "Records created successfully";
    return true;
  }

  public static function delete_records($database, $tableName, $whereFields) {
    //["category_uid" => "drink"]
    $whereColumns = [];
    foreach ($whereFields as $whereColumnName => $whereColumnValue) {
      array_push($whereColumns, $whereColumnName . "=" . $whereColumnValue);
    }

    $sql = "DELETE FROM " . $tableName . " WHERE " . join(" AND ",$whereColumns) . ";";

    $returnValue = $database->exec($sql);
    if (!$returnValue) {
      //return database->lastErrorMsg();
      return false;
    }

    //echo "Records created successfully";
    return true;
  }
}

?>
