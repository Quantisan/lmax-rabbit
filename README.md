# lmax-rabbit

lmax-rabbit is a thin client over RabbitMQ to control and trade foreign exchange through LMAX brokerage. The purpose is to decouple the LMAX Java API from the trading system so you can use any programming language for your trading logic. This was developed as part of a polyglot trading system which has since been abandoned as I am no longer trading or developing trading system. Use this at your own risk for I do not provide any guarantee or support.

## Usage

The main class is in [ThinBot.java](src/main/java/quantisan/qte_lmax/ThinBot.java) which listens on RabbitMQ for work instructions. See [OrderTest.java](src/test/java/quantisan/qte_lmax/OrderTest.java) for examples of placing orders under the hood of message handling. lmax-rabbit uses EDN data scheme as messages. See [EdnMessageTest.java](src/test/java/quantisan/qte_lmax/EdnMessageTest.java) for the message schema that you should send over RabbitMQ.

### Production

```
java -jar -Dquantisan.logging.production=true qte-lmax-1.0-one-jar.jar
```