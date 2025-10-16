TASK :
[ ] pointer email 
[ ] analyze file for filtering the customers

Come avviare per caricare le variabili .env : 

- Linux/macOs :
export $(cat .env | xargs)
./mvn spring-boot:run

- Windows :
Get-Content .env | ForEach-Object {
  if ($_ -match "^(.*)=(.*)$") { setx $($matches[1]) $($matches[2]) }
}
mvn spring-boot:run