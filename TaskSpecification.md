# Obrada i analiza meteoroloških podataka

Zadatak je implementirati sistem koji se sastoji od više konkurentnih niti za obradu velikih tekstualnih datoteka (sa ekstenzijom .txt i .csv) u zadatom direktorijumu. Sistem treba da prati promene u tom direktorijumu, obradi fajlove koji sadrže podatke o meteorološkim stanicama i zabeleženim temperaturama, i da omogućava korisniku da putem komandne linije pokreće dodatne zadatke i proverava status izvršenja poslova.

Podaci u fajlovima su organizovani po sledećem formatu (svaka linija predstavlja jedno merenje):

`Hamburg;12.0`

`Bulawayo;8.9`

`Palembang;38.8`

`St. John's;15.2`

`Cracow;12.6`

`Bridgetown;26.9`

`Istanbul;6.2`

`Roseau;34.4`

`Conakry;31.2`

`Istanbul;23.0`

## 1 Komponente sistema

### 1.1 Nit za monitoring direktorijuma

Osluškuje zadati direktorijum (definisan putem komandne linije ili konfiguracionog fajla) i detektuje nove ili izmenjene fajlove sa ekstenzijom .txt i .csv.

Za svaki fajl se beleži “last modified” vrednost – ukoliko je fajl već obrađen (prethodna i trenutna vrednost su iste), novi posao se ne pokreće.  
Čitanje fajlova vrši se deo po deo (ne smeštati čitav fajl u memoriju) zbog potencijalno ogromne veličine fajlova (measurements\_big.txt je \~ 14GB).

Kada se detektuje promena u direkotrijumu potrebno je ispisati poruku koji fajlovi su se izmenili i/ili dodali.

### 

### 1.2 Obrada fajlova i ažuriranje in-memory mape

Kada se otkrije promena (i kada se prvi putpokrene) pokreće se posao (preko ExecutorService) koji obrađuje sve fajlove unutar direktorijuma. Svaka nit unutar ExecutorService-a treba da radi na jednom fajlu. Preporuka je koristiti 4 niti unutar servisa.

Svaka linija u fajlu sadrži naziv meteorološke stanice i zabeleženu temperaturu. Podaci se kombinuju u in-memory mapi organizovanoj abecedno, gde se za svako slovo (prvo slovo naziva stanice) čuva: broj stanica koje počinju tim slovom i suma svih merenja za te stanice.  
Ovaj zadatak obrade fajlova koji ažurira mapu mora biti zaštićen mehanizmima sinhronizacije kako se ne bi sudarila sa drugim operacijama čitanja istih fajlova.  
Napomena: U slučaju obrade CSV fajla potrebno je preskočiti zaglavlje.  
Napomena: Smatra se da će sadržaj svih fajlova unutar direktorijuma biti u korektnom formatu.

### 1.3 CLI nit i obrada komandi

Korisnik unosi komande putem komandne linije. Sve komande se upisuju u blokirajući red (`STOP` i `START` ne), a posebna nit periodično čita iz tog reda i delegira zadatke.

Komande imaju argumente koje se se mogu zadati u bilo kom redosledu i moraju biti označene prefiksom “--” (dugi oblik) ili jednostavnom crticom “-” (kratki oblik). `K`omanda se može napisati kao:  
`SCAN --min 10.0 --max 20.0 --letter H --output output.txt --job job1`  
ili kratko:  
`SCAN -m 10.0 -M 20.0 -l H -o output.txt -j job1`

Sistem mora validirati sve primljene komande te, ukoliko je neka komanda neispravna ili nedostaju potrebni argumenti, ispisati jasnu grešku (bez stack trace-a) i nastaviti rad.

CLI nit ni u jednom trenutku ne sme biti blokirana.

#### 

#### 1.3.1 Komanda `SCAN`

Pretražuje sve fajlove u nadgledanom direktorijumu i pronalazi meteorološke stanice čiji naziv počinje zadatim slovom i za koje je temperatura u opsegu \[min, max\]. Svaki fajl treba da se obradi sa jednom niti unutar ExecutorService-a, gde se oni kasnije upisuju u izlazni fajl (output) čije se ime zadaje kao argument komande `SCAN`.   
Voditi računa da se rezultati pronalaženja stanica za fajlove ne čuvaju i ne kombinuju u memoriji jer se radi sa velikim fajlovima ([Java OutOfMemoryError](https://docs.oracle.com/javase/8/docs/technotes/guides/troubleshoot/memleaks002.html)).  
*Argumenti:*  
`--min` (ili `-m`): minimalna temperatura  
`--max` (ili `-M`): maksimalna temperatura  
`--letter` (ili `-l`): početno slovo meteorološke stanice  
`--output` (ili `-o`): naziv izlaznog fajla  
`--job` (ili `-j`): naziv zadatka  
*Primer:*  
`SCAN --min 10.0 --max 20.0 --letter H --output output.txt --job job1`  
*Output fajl linija:*  
`Hamburg;12.0`

#### 1.3.2 Komanda `STATUS`

Prikazuje trenutni status zadatka (pending, running ili completed) sa navedenim imenom.  
*Argumenti:*  
`--job` (ili `-j`): naziv zadatka  
*Primer:*  
`STATUS --job job1`  
`job1 is running`

#### 1.3.3 Komanda `MAP`

Ispisuje sadržaj in-memory mape – u 13 linija, gde svaka linija prikazuje po dva slova sa pripadajućim brojem stanica i sumom merenja. Voditi računa o situaciji kada je mapa nedostupna, u trenutnku kada se prvi put upisuju vrednosti, tada je potrebno ispisati poruku da mapa još uvek nije dostupna.  
*Primer:*  
`MAP`  
`a: 8524 - 1823412 | b: 5234 - 523512`  
`c: 8523 - 5521342 | d: 1253 - 502395 …`

#### 1.3.4 Komanda `EXPORTMAP`

Eksportuje sadržaj in-memory mape u log CSV fajl. CSV fajl sadrži kolone: "Letter", "Station count", "Sum".  Svaki red log fajla, koji se čuva u okviru projekta na proizvoljnoj lokaciji, treba da sadrži podatke u formatu:  
`a 8524 1823412`  
`b 5234 523512 …`  
*Primer:*  
`EXPORTMAP`

#### 1.3.4 Komanda `SHUTDOWN`

Na elegantan način zaustavlja ceo sistem – prekida sve ExecutorService-ove i signalizira svim nitima da uredno završe rad. Pored toga, ako se doda opcija:  
`--save-jobs` (ili `-s`), svi neizvršeni poslovi se sačuvaju u poseban `load_config` fajl. Neizvršene poslove možete čuvati u bilo kom formatu.  
*Primer:*  
`SHUTDOWN --save-jobs`

#### 1.3.5 Komanda `START`

Pokreće sistem. Dodatna opcija `--load-jobs` (ili `-l`) omogućava da se, ako postoji fajl (`load_config`) sa sačuvanim zadacima, ti poslovi učitaju i automatski dodaju u red za izvršavanje. Ukoliko posao ne može biti započet, jer na primer neko drugi radi sa fajlom, nije dozvoljeno odbaciti taj posao.  
*Primer:*  
`START --load-jobs`

### 

### 1.4 Periodični izveštaj

Potrebno je implementirati dodatnu nit koja će, svakog minuta, generisati automatski izveštaj o trenutnom stanju in-memory mape i upisivati ga u log CSV fajl (isti fajl koji se koristi za `EXPORTMAP`) komandu. Vodite računa da se izveštaj ne meša, odnosno ne izvršava istovremeno, sa ručnim logovanjem (preko komande `EXPORTMAP`).

### 1.5 Izvršavanje zadataka preko ExecutorService

Poslove koji se odnose na obradu fajlova (npr. čitanje fajlova, pretraga podataka) podelite na manje zadatke koji se paralelno izvršavaju. Preporučeno je korišćenje fork/join pool-a ili klasičnog thread pool-a (minimum 4 niti).

Svaki zadatak mora imati jedinstveni identifikator (naziv zadatka) radi praćenja statusa.

## 2 Tehnički zahtevi i smernice

* Fajlovi se čitaju deo po deo (korišćenjem streamova ili BufferedReadera) kako se ne bi preopteretila memorija.
* Koristite odgovarajuće mehanizme da osigurate da se operacije nad fajlovima i ažuriranje mape ne izvršavaju istovremeno sa operacijama pretrage i izvoza rezultata.
* Sistem NE sme da pukne ni u jednom trenutku.  
  Svi izuzeci se moraju obraditi kulturno – ispisati korisniku kratke, jasne poruke (npr. "Greška pri čitanju fajla 'naziv\_fajla'. Nastavljam rad."), bez prikazivanja kompletnog stack trace-a.
* Argumenti za komande mogu biti zadati u bilo kojem redosledu, a prepoznaju se pomoću prefiksa (npr. `--min` ili `-m`, `--output` ili `-o`, itd.). Sistem mora validirati argumente i u slučaju greške obavestiti korisnika bez rušenja sistema.
* Komanda **`SHUTDOWN`** mora da prekine sve niti na uredan način. Ukoliko se doda opcija `--save-jobs`, svi neizvršeni poslovi se sačuvaju u posebnom fajlu.
* Preporuka je da se svi poslovi upisuju u centralni blokirajući red. Komponente kao što su nit koja detektuje izmene u direktorijumu i nit koja prihvata komande sa komandne linije dodaju poslove, dok posebna, druga nit preuzima poslove i delegira ih dalje.

## 

## 3 Rok i predaja projekta

Rok za izradu domaćeg za sve grupe (uključujući i studente koji ne slušaju predmet prvi put) je **28.3.2025**. do **23.59h.**

Projekat je potrebno nazvati i zipovati u formatu:  
kids\_d1\_ime\_prezime\_brojindexa\_godinaupisa\_smer  
Primer:  
kids\_d1\_petar\_petrovic\_01\_22\_rn

Finalni projekat potrebno je zipovati **BEZ** foldera data, u kome su testni primeri, i okačiti na Google form-u na linku: [https://forms.gle/tf1BWhWoZiEpxmHT6](https://forms.gle/tf1BWhWoZiEpxmHT6)

## 4 Bodovanje \- ukupno 20 poena

Svi delovi koda se boduju parcijalno. Da bi se poeni uvažili potrebno je demonstrirati uraðeni deo.  
**\- Nadgledanje direktorijuma i obrada fajlova:** 5 poena

* Sistem pravilno osluškuje zadati direktorijum (definisan putem komandne linije ili konfiguracionog fajla) i detektuje nove ili izmenjene fajlove sa ekstenzijom .txt i .csv.
* Fajlovi se čitaju liniju po liniju ili u manjim segmentima, bez učitavanja celog fajla u memoriju (bitno za velike fajlove).
* Sistem pravilno preskače zaglavlje u CSV fajlovima.

**\- Obrada fajlova i ažuriranje in-memory mape:** 5 poena

* Korišćenje ExecutorService-a gde svaka nit obrađuje jedan fajl.
* Kombinovanje podataka u in-memory mapu organizovanoj abecedno, gde se za svako slovo evidentira broj stanica i suma svih merenja.
* Implementirati mehanizam sinhronizacije koji osigurava da se operacije ažuriranja mape ne sudaraju sa pretragom ili eksportom podataka.

**\- CLI nit i fleksibilno parsiranje argumenata:** 2 poena

* Nit koja čita komande iz blokirajućeg reda, bez blokiranja korisničkog interfejsa.
* Podržavanje unosa argumenata u bilo kojem redosledu uz prefikse – dugi oblik (npr. `--min`, `--output`, `--job`) i kratki oblik (npr. `-m`, `-o`, `-j`).
* Sistem validira primljene argumente i u slučaju greške ispisuje jasne poruke (bez prikazivanja kompletnog stack trace-a) te nastavlja rad.

**\- Komande SCAN, STATUS, MAP, EXPORTMAP:** 6 poena  
\- **Periodični izveštaj:** 2 poena

**Negativni poeni**  
\- Nema elegantno gašenja i program se ne završava: \-2 poena  
\- Program baca exception (ili stampa stack-trace) koje prekidaju normalan rad: \-2 poena

