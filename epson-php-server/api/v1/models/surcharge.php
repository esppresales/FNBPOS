<?php
include_once('db_helpers.php');

class Surcharge {
  public static function get_all_surcharges($database) {
    return DB::get_all_records($database, SURCHARGES_TABLE_NAME);
  }

  public static function create_surcharge($database, $name, $percentage, $priority) {
    $fields = [
      "name" => $name,
      "percentage" => $percentage,
      "priority" => $priority
    ];

    if (DB::create_new_record($database, SURCHARGES_TABLE_NAME, $fields)) {
      $surcharge_id = $database->lastInsertRowid();
      return Surcharge::get_all_surcharges($database);
    }

    return false;
  }

  public static function delete_all_surcharges($database) {
    return DB::delete_all_records($database, SURCHARGES_TABLE_NAME);
  }
}

?>
