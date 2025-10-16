TASK :
[ ] pointer email little try with db
[x] analyze file for filtering the customers
[ ] documentation (add some graph)
[ ] whitelist the domain

Come avviare per caricare le variabili .env : 

- Linux/macOs :
export $(cat .env | xargs)
./mvn spring-boot:run

- Windows :
Get-Content .env | ForEach-Object {
  if ($_ -match "^(.*)=(.*)$") { setx $($matches[1]) $($matches[2]) }
}
mvn spring-boot:run


Temporanea0