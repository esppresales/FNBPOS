<?php
$filename = 'c:\php_errors.log';

if (file_exists($filename)) {
  unlink($filename);
}

?>
