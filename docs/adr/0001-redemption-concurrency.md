# ADR-0001: Kontrola współbieżności wykorzystania kuponu

- Status: accepted
- Data: 2026-07-24

## Kontekst

Wiele żądań może równocześnie próbować użyć tego samego kuponu. System nie może:

- zwiększyć `current_uses` powyżej `max_uses`,
- dopuścić dwóch użyć kuponu przez tego samego użytkownika,
- zapisać licznika bez odpowiadającego mu rekordu wykorzystania lub odwrotnie,
- opierać poprawności na blokadzie lokalnej dla jednej instancji JVM.

Serwis ma pozostać stateless i możliwy do skalowania horyzontalnego.

## Decyzja

Dla operacji wykorzystania kuponu stosujemy krótką transakcję PostgreSQL oraz blokadę wiersza kuponu przez JPA `PESSIMISTIC_WRITE`.

Przepływ w transakcji:

1. pobranie kuponu po kanonicznym kodzie z blokadą,
2. sprawdzenie w aplikacji, czy użytkownik wcześniej wykorzystał kupon,
3. sprawdzenie kraju i limitu w modelu domenowym,
4. zwiększenie licznika,
5. zapis rekordu wykorzystania,
6. commit obu zmian; constraint `UNIQUE(coupon_id, user_id)` pozostaje ostatnią linią obrony.

Wywołanie dostawcy GeoIP odbywa się przed transakcją. Po pobraniu wiersza kraj jest sprawdzany ponownie w domenie, więc zmiana danych poza normalnym API nie omija reguły.

## Konsekwencje pozytywne

- brak oversellingu kuponu przy wielu wątkach i wielu instancjach,
- prosta semantyka i łatwe dowodzenie poprawności,
- atomowa aktualizacja licznika i historii,
- brak dodatkowego systemu blokad,
- ograniczenie unikalności w DB pozostaje ostatnią linią obrony.

## Konsekwencje negatywne

- wszystkie użycia jednego kuponu są serializowane,
- bardzo popularny kupon może stać się hot row,
- czas odpowiedzi rośnie wraz z kolejką oczekującą na blokadę,
- kolejność przyznania blokady zależy od schedulera i bazy; nie jest formalnym globalnym FIFO od chwili nadejścia HTTP.

## Rozważone alternatywy

### Blokada `synchronized` lub `ReentrantLock`

Odrzucona, ponieważ chroni tylko jedną JVM i przestaje być poprawna po uruchomieniu drugiej instancji.

### Optimistic locking przez `@Version`

Poprawny wariant, ale przy hot row powodowałby dużą liczbę konfliktów, retry i mniej przewidywalną latencję. Wymagałby też precyzyjnej polityki retry.

### Atomowy warunkowy SQL

Przykładowo:

```sql
UPDATE coupons
SET current_uses = current_uses + 1
WHERE id = :id
  AND current_uses < max_uses
RETURNING current_uses, max_uses;
```

To rozwiązanie może zwiększyć przepustowość i zmniejszyć liczbę round-tripów. Komplikuje jednak rozróżnienie „już użyty przez użytkownika” od „limit wyczerpany” oraz atomowe połączenie insertu historii z licznikiem. Jest naturalnym kolejnym krokiem po potwierdzeniu problemu testami obciążeniowymi.

### Redis/distributed lock

Odrzucona. Dodaje nowy system koordynacji i scenariusze awarii, mimo że PostgreSQL już przechowuje stan wymagający atomowej zmiany.

## Kryterium ponownego rozpatrzenia

Decyzję należy zrewidować, gdy pomiary pokażą, że pojedynczy kupon generuje obciążenie, którego nie obsługuje akceptowalna kolejka blokad. Wtedy preferowaną alternatywą jest atomowy SQL z testami zachowania wszystkich inwariantów.
