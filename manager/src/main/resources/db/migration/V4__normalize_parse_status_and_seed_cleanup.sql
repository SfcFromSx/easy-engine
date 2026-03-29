UPDATE sql_execution_record
SET parse_status = 'OK'
WHERE parse_status = 'PARSED';
