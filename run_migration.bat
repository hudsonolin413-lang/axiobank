@echo off
echo Running database migration...
psql -h localhost -p 5433 -U postgres -d AxionBank -f database/migrations/add_account_number_to_sub_accounts.sql
echo Migration complete!
pause
