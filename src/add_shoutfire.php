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

   $message = $_POST['message_content'];
   $nickname = $_POST['nickname'];

   $username = 'garebare_shout';
   $password = 'y8bysfsyz';
   $database = 'garebare_shoutfire';
   $link = mysql_connect("localhost", $username, $password);
   if(!$link) echo "Could not connect";

   $status = mysql_select_db($database);
   if(!$status) echo "Could not select";

   $date = date('Y-m-d H:i:s');
   $query = "INSERT INTO messages (content, timestamp, nickname) VALUES (\"$message\",\"$date\",\"$nickname\")";
   $result = mysql_query($query);
   $query_error = mysql_error();   

   mysql_close();

   echo '<?xml version="1.0" ?>';
?>
<info>
   <success><?php if(!$result) echo "False"; else echo "True"; ?></success>
   <errorMsg><?php if(!$result) echo $query_error; else echo "Success"; ?></errorMsg>
</info>
