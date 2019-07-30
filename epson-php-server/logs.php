<?php
$filename = 'c:\php_errors.log';

if (file_exists($filename)) {
    $handle = fopen($filename, "r");
    header("Content-Type: text/plain");

    if ($handle) {
        for($x_pos = 0, $ln = 0, $output = array(); fseek($handle, $x_pos, SEEK_END) !== -1; $x_pos--) {
            $char = fgetc($handle);
            if ($char === "\n") {
                // analyse completed line $output[$ln] if need be
                echo $output[$ln];
                $ln++;
                continue;
            }
            $output[$ln] = $char . ((array_key_exists($ln, $output)) ? $output[$ln] : '');
        }
        /*
        while (($line = fgets($handle)) !== false) {
            echo $line;
        }
        */
    } else {
        // error opening the file.
    }
    fclose($handle);
}
?>
