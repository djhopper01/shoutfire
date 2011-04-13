<?php

   header("Content-Type: text/xml");
   
   $correct_secret = "0E22C7E9A1080EF7A1726F94FF2CF4478F46DC473336025EC05A003B365E5487";
   $correct_key = "Lh6mz83XsBuqU7p4rYZMismVvICEtftCvvnQ540BrN2ev7keIaaOYYrukMFNxgJ";

   $secret = $_POST['secret'];
   $key = $_POST['key'];

   if($secret != $correct_secret)
      die();

   if($key != $correct_key)
      die();

   $message = $_POST['count'];

   $username = 'garebare_shout';
   $password = 'y8bysfsyz';
   $database = 'garebare_shoutfire';
   $link = mysql_connect("localhost", $username, $password);
   if(!$link) echo "Could not connect";

   $status = mysql_select_db($database);
   if(!$status) echo "Could not select";

   $date = date('Y-m-d H:i:s');
   $query = "SELECT content, timestamp, nickname FROM messages ORDER BY timestamp DESC";
   $result = mysql_query($query);
   $query_error = mysql_error();   

   echo '<?xml version="1.0" ?>';
   echo '<messages>';

   for($counter = 0; $counter < 100; $counter += 1) {
     if(($row = mysql_fetch_assoc($result)) == false)
       break;
     echo '<message>';
     echo '<content>';
     echo $row['content'];
     echo '</content>';
     echo '<timestamp>';
     echo $row['timestamp'];
     echo '</timestamp>';
     echo '<nickname>';
     echo $row['nickname'];
     echo '</nickname>';
     echo '</message>';
   }

   echo '</messages>';

   mysql_close();
?>
