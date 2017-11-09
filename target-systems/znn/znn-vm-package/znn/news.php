<html>
<head>
<title>ZNN News</title>
</head>
<body>
<?php

#
# Edit this to configure a different database server.
#
$db_username = "my_znn";
$db_password = "my_znn_pass";
$db_name = "znn_data";
$db_host = "10.5.6.2";
$db_port = "3306";


#
# Read and parse fidelity file (if available).
#
$server_port = $_SERVER["SERVER_PORT"];
$server_ip = $_SERVER["SERVER_ADDR"];
$fidelity_file_name = "/tmp/znn-fidelity-$server_port";
if (file_exists($fidelity_file_name)) {
	$fidelity_file = fopen($fidelity_file_name, "r");
	$line = trim(fgets($fidelity_file));
	fclose($fidelity_file);
	if ($line == "low") {
		$fidelity = "low";
		$fidelity_info = "low (specified by fidelity file)";
	} elseif ($line == "text") {
		$fidelity = "text";
		$fidelity_info = "text (specified by fidelity file)";
	} elseif ($line == "high") {
		$fidelity = "high";
		$fidelity_info = "high (specified by fidelity file)";
	} else {
		$fidelity = "high";
		$fidelity_info = "high (invalid fidelity file contents: $line)";
	}
} else {
	$fidelity = "high";
	$fidelity_info = "high (fidelity file not found)";
}

#
# Connect to the database and count how many news are there.
#
$dbh = new PDO("mysql:host=$db_host;port=$db_port;dbname=$db_name",
		$db_username, $db_password);

$sth = $dbh->query("select count(*) from news");
$row = $sth->fetch();
$news_count = $row[0];

#
# Pick a random news and get its data.
#
$news_pos = rand(0, $news_count - 1);

$sth = $dbh->query("select news_id from news");
for ($i = 0; $i <= $news_pos; $i++) {
	$row = $sth->fetch();
}

$news_id = $row[0];

$sth = $dbh->query("select news_title, news_text, news_img_cnt from news
		where news_id = $news_id");
$row = $sth->fetch();
$news_title = $row[0];
$news_text = "<p>" . str_replace("\n\n", "</p><p>", $row[1]) . "</p>";
$news_img_cnt = $row[2];

#
# Print the news.
#
echo "
	<h1>$news_title</h1>
	$news_text
	<hr>";

#
# Print the images depending on the fidelity mode.
#
if ($fidelity == "text") {
	echo "<p>No images served in text fidelity mode.</p>";
} else {
	if ($fidelity == "high") {
		$img_field = "img_high_res";
	} else {
		$img_field = "img_low_res";
	}
	
	$sth = $dbh->query("select img_id, $img_field from img
			where news_id = $news_id order by img_id");
	for ($i = 0; $i < $news_img_cnt; $i++) {
		$row = $sth->fetch();
		echo "<p><img src=\"images/" . $row[1] . "\"/>";
		echo "<br>";
		echo "<small>Image " . $row[0] . ", name = " . $row[1] . "</small></p>";
	}
}

#
# Make some random delay if required.
#
$random_delay_file_name="/tmp/znn-delay";
$random_delay_text="(random delay not enabled, file $random_delay_file_name 
		does not exist)";
$random_delay_mb="0";
if (file_exists($random_delay_file_name)) {
	$random_delay_file = fopen($random_delay_file_name, "r");
	$random_delay_mb = trim(fgets($random_delay_file));
	fclose($random_delay_file);
	
	$random_delay_output = shell_exec("dd if=/dev/urandom bs=1048576 " .
			"count=$random_delay_mb 2>/dev/null | sha1sum | cut -d \" \" -f 1");
	$random_delay_text="(random delay enabled: SHA-1 of ${random_delay_mb}Mb
			of random data is $random_delay_output).";
}

#
# Print the footer with some diagnostic information.
#
echo "<hr>
	<p><small>ZNN fake news service. Server by $server_ip:$server_port.
		Fideliy level $fidelity_info defined in
		file $fidelity_file_name.</small></p>
	<p><small>Total news in database: $news_count. Printing news $news_pos
		with ID $news_id (news has $news_img_cnt images).</small></p>
	<p><small>$random_delay_text</small></p>
	";


?>

</body>
</html>
