<?php

function copy_uploaded_file($uploaded_file, $target_path, $success_callback, $failure_callback) {
  if (!file_exists(dirname($target_path))) {
      mkdir(dirname($target_path), 0777, true);
  }

  if(move_uploaded_file($uploaded_file['tmp_name'], $target_path)) {
      //echo "The file ".  basename($image_file['name']) . " has been uploaded";
      $success_callback();
  } else{
      // $response["success"] = 0;
      // $response["message"] = "Database Error. Couldn't upload file.";
      // die(json_encode($response));
      $failure_callback();
  }
}

function list_files_in_directory($directory) {
  $files = scandir($directory);
  return $files;
}

?>
