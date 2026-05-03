# Debezium Outbox Connectors

Outbox pattern'inin Kafka tarafına çıkışını yapan Debezium connector config'leri. Her **outbox tablosu için ayrı bir connector** dosyası.

## Mevcut Connector'lar

| Dosya | İzlenen Tablo | Replication Slot | Publication | Üretilen Topic |
|---|---|---|---|---|
| [order-outbox-connector.json](order-outbox-connector.json) | `public.outbox_order` | `outbox_order_slot` | `outbox_order_pub` | `order.events` |
| [stock-outbox-connector.json](stock-outbox-connector.json) | `public.outbox_stock` | `outbox_stock_slot` | `outbox_stock_pub` | `stock.events` |
| [payment-outbox-connector.json](payment-outbox-connector.json) | `public.outbox_payment` | `outbox_payment_slot` | `outbox_payment_pub` | `payment.events` |

---

## Deploy

### Önkoşullar

1. **PostgreSQL `wal_level=logical`** olmalı:
   ```bash
   docker exec postgres psql -U postgres -c "SHOW wal_level;"
   # logical
   ```
   `debezium/postgres:16` image'ı default olarak `logical` ile gelir.

2. **Kafka Connect ayakta** ve REST API erişilebilir (port 8083).

3. **Outbox tabloları oluşturulmuş** olmalı (Hibernate `ddl-auto: update` ile servis ilk başlatıldığında oluşur).

### Tek bir connector deploy etme

```bash
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @order-outbox-connector.json
```

### Hepsini bir kerede deploy etme

```bash
for f in *-outbox-connector.json; do
  echo "Deploying $f..."
  curl -X POST http://localhost:8083/connectors \
    -H "Content-Type: application/json" \
    -d @"$f"
  echo
done
```

---

## Doğrulama

### Connector durumu

```bash
# Liste
curl http://localhost:8083/connectors

# Tek connector durumu
curl http://localhost:8083/connectors/order-outbox-connector/status
```

Beklenen:
```json
{
  "name": "order-outbox-connector",
  "connector": { "state": "RUNNING", "worker_id": "..." },
  "tasks": [ { "id": 0, "state": "RUNNING" } ]
}
```

## Yeniden Başlatma / Silme

### Restart (config değişikliğinden sonra)

```bash
curl -X POST http://localhost:8083/connectors/order-outbox-connector/restart
```

### Connector silme

```bash
curl -X DELETE http://localhost:8083/connectors/order-outbox-connector
```

## Yeni Servise Connector Ekleme

1. `<service>-outbox-connector.json` dosyasını mevcut birinden kopyala
2. Şu 4 alanı güncelle:
   - `name`: `<service>-outbox-connector`
   - `database.server.name`: `<service>-service`
   - `table.include.list`: `public.outbox_<service>`
   - `publication.name`: `outbox_<service>_pub`
   - `slot.name`: `outbox_<service>_slot`
3. Servisin entity'sinde `OutboxEvent` thin subclass'ı `@Table(name = "outbox_<service>")` ile oluştur.
4. Deploy: `curl -X POST ... -d @<service>-outbox-connector.json`

---