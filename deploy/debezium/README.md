# Debezium Connectors

1. **Outbox source connectors** — saga için. `*_outbox` tablolarını dinler, EventRouter SMT ile `<aggregate>.events` topic'ine semantik event basar.
2. **Product CDC source + ES sink** — search için. `public.products` tablosunu dinler, `products` topic'ine state'i yansıtır; Confluent Elasticsearch Sink Connector aynı topic'i Elasticsearch'e indexler.

## Plugin Yönetimi — Custom Image (Dockerfile)

`debezium-connect` servisi, bu klasördeki [Dockerfile](Dockerfile) ile üretilen custom image'ı kullanır (`n11/debezium-connect:2.7.3-es14.1.7`). Multi-stage build:

```
Stage 1: confluentinc/cp-kafka-connect-base:7.7.0
   └─ confluent-hub install confluentinc/kafka-connect-elasticsearch:14.1.7
        └─ /plugins/confluentinc-kafka-connect-elasticsearch

Stage 2: debezium/connect:2.7.3.Final
   └─ COPY --from=stage1 /plugins → /kafka/connect
         CONNECT_PLUGIN_PATH=/kafka/connect
```

Stage 1 yalnızca `confluent-hub` CLI'yı barındıran transient build stage'i; final image'a sadece extracted plugin klasörü kopyalanır. Final image Debezium runtime + Confluent ES Sink hazır.

**Build:** `docker compose build debezium-connect`.

**Yeni plugin eklemek için:** Dockerfile'ın stage 1'ine ek `RUN confluent-hub install ...` ekle, `compose build` ile yeniden bas. Tag suffix'ini de güncelle (örn `2.7.3-es14.1.7-jdbc1.2`).

`connector-init` ile akış:
| Container | Görev | Tetiklenme |
|---|---|---|
| `debezium-connect` (custom image) | Connect runtime + plugin'ler | infra healthy olduğunda |
| `connector-init` | Connector JSON'larını Debezium'a POST et | debezium-connect + app servisleri healthy olunca |

## Mevcut Connector'lar

### Outbox source (saga)

| Dosya | İzlenen Tablo | Replication Slot | Publication | Üretilen Topic |
|---|---|---|---|---|
| [order-outbox-connector.json](order-outbox-connector.json) | `public.outbox_order` | `outbox_order_slot` | `outbox_order_pub` | `order.events` |
| [stock-outbox-connector.json](stock-outbox-connector.json) | `public.outbox_stock` | `outbox_stock_slot` | `outbox_stock_pub` | `stock.events` |
| [payment-outbox-connector.json](payment-outbox-connector.json) | `public.outbox_payment` | `outbox_payment_slot` | `outbox_payment_pub` | `payment.events` |

### Product CDC source + ES Sink (search projection)

| Dosya | Görev |
|---|---|
| [product-cdc-connector.json](product-cdc-connector.json) | PostgreSQL `public.products` → Kafka topic `products`. SMT zinciri: `unwrap` (envelope dış) + `rename` (snake_case → camelCase) + `routeTopic` (tek topic adı). |
| [products-es-sink-connector.json](products-es-sink-connector.json) | Kafka topic `products` → Elasticsearch index `products`. Confluent Elasticsearch Sink Connector 14.1.7 (custom image'da gömülü). |

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