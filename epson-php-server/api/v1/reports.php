<?php
include_once('../../conf/constant.php');
include_once('db_helpers.php');
include_once('request_helpers.php');
include_once('response_helpers.php');
include_once('models/receipt.php');

$database = DB::get_database();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
  $receipts = [];
  if (isset($_GET['start_time']) && isset($_GET['end_time'])) {
    $start_time = $_GET['start_time'];
    $end_time = $_GET['end_time'];

    $receipts = Receipt::get_receipts_by_time_range($database, $start_time, $end_time);
  }
  else {
    $receipts = Receipt::get_all_receipts($database);
  }


  $result = [];

  $eod_result = [];
  $monthly_result = [];

  foreach ($receipts as $receipt) {
    $date_obj = new DateTime($receipt['paid_at']);
    $receipt_date = $date_obj->format('Y-m-d');
    $receipt_month = $date_obj->format('Y-m');
    $receipt_hour = $date_obj->format('H');
    $receipt_day = $date_obj->format('d');

    if (!array_key_exists($receipt_date, $eod_result)) {
      $eod_result[$receipt_date] = [
        'day' => $date_obj->format('d'),
        'month' => $date_obj->format('m'),
        'year' => $date_obj->format('Y'),
        'total_amount' => 0.0,
        'total_order_count' => 0,
        'average_amount_per_order' => 0.0,
        'amount_by_hour' => [],
        'order_count_by_hour' => [],
        'product_ordered_count' => []
      ];
    }

    if (!array_key_exists($receipt_month, $monthly_result)) {
      $monthly_result[$receipt_month] = [
        'month' => $date_obj->format('m'),
        'year' => $date_obj->format('Y'),
        'total_amount' => 0.0,
        'total_order_count' => 0,
        'average_amount_per_day' => 0.0,
        'average_orders_per_day' => 0,
        'amount_by_day' => [],
        'order_count_by_day' => [],
        'product_ordered_count' => []
      ];
    }


    foreach($receipt['orders'] as $order) {
      // monthly, total_order_count
      $monthly_result[$receipt_month]['total_order_count'] += 1;

      // eod, total_order_count
      $eod_result[$receipt_date]['total_order_count'] += 1;

      // monthly, order_count_by_day
      if (!array_key_exists($receipt_day, $monthly_result[$receipt_month]['order_count_by_day'])) {
        $monthly_result[$receipt_month]['order_count_by_day'][$receipt_day] = 0;
      }
      $monthly_result[$receipt_month]['order_count_by_day'][$receipt_day] += 1;

      // eod, order_count_by_hour
      if (!array_key_exists($receipt_hour, $eod_result[$receipt_date]['order_count_by_hour'])) {
        $eod_result[$receipt_date]['order_count_by_hour'][$receipt_hour] = 0;
      }
      $eod_result[$receipt_date]['order_count_by_hour'][$receipt_hour] += 1;

      foreach($order['order_items'] as $order_item) {
        $product_uid = $order_item['product_uid'];
        $quantity_served = $order_item['quantity_served'];

        // monthly, product_ordered_count
        if (!array_key_exists($product_uid, $monthly_result[$receipt_month]['product_ordered_count'])) {
          $monthly_result[$receipt_month]['product_ordered_count'][$product_uid] = 0;
        }
        $monthly_result[$receipt_month]['product_ordered_count'][$product_uid] += $quantity_served;

        // eod, product_ordered_count
        if (!array_key_exists($product_uid, $eod_result[$receipt_date]['product_ordered_count'])) {
          $eod_result[$receipt_date]['product_ordered_count'][$product_uid] = 0;
        }
        $eod_result[$receipt_date]['product_ordered_count'][$product_uid] += $quantity_served;

        // monthly, total_amount
        $monthly_result[$receipt_month]['total_amount'] += $receipt['final_amount'];

        // eod, total_amount
        $eod_result[$receipt_date]['total_amount'] += $receipt['final_amount'];

        // monthly, amount_by_day
        if (!array_key_exists($receipt_day, $monthly_result[$receipt_month]['amount_by_day'])) {
          $monthly_result[$receipt_month]['amount_by_day'][$receipt_day] = 0;
        }
        $monthly_result[$receipt_month]['amount_by_day'][$receipt_day] += $receipt['final_amount'];

        // eod, amount_by_hour
        if (!array_key_exists($receipt_hour, $eod_result[$receipt_date]['amount_by_hour'])) {
          $eod_result[$receipt_date]['amount_by_hour'][$receipt_hour] = 0;
        }
        $eod_result[$receipt_date]['amount_by_hour'][$receipt_hour] += $receipt['final_amount'];
      }
    }

    // monthly, average_amount_per_day and average_orders_per_day
    foreach($monthly_result as $month => $month_result) {
      $day_count = count($month_result['amount_by_day']);
      if ($day_count > 0) {
        $average_amount_per_day = $monthly_result[$month]['total_amount'] / $day_count;
        $monthly_result[$month]['average_amount_per_day'] = $average_amount_per_day;
        $average_order_count_per_day = $monthly_result[$month]['total_order_count'] / $day_count;
        $monthly_result[$month]['average_orders_per_day'] = $average_order_count_per_day;
      }
    }

    // eod, average_amount_per_order
    foreach($eod_result as $date => $date_result) {
      $eod_result[$date]['average_amount_per_order'] = $date_result['total_amount'] / $date_result['total_order_count'];
    }
  }

  $result["eod"] = $eod_result;
  $result['monthly'] = $monthly_result;

  header('Content-Type: application/json');
  echo json_encode($result, JSON_UNESCAPED_SLASHES);
}
else {
  invalid_response();
}

?>
