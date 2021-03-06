# study-micro-services-distribute-transaction #

## 参考链接 ##
- 聊聊分布式事务，再说说解决方案 [https://www.cnblogs.com/savorboard/p/distributed-system-transaction-consistency.html](https://www.cnblogs.com/savorboard/p/distributed-system-transaction-consistency.html)
- 第一次有人把“分布式事务”讲的这么简单明了 [https://www.jianshu.com/p/16b1baf015e8](https://www.jianshu.com/p/16b1baf015e8)
- 微服务架构下分布式事务解决方案——阿里GTS [https://www.cnblogs.com/jiangyu666/p/8522547.html](https://www.cnblogs.com/jiangyu666/p/8522547.html)

## 两阶段提交（2PC） ##
在 XA 协议中分为两阶段：

事务管理器要求每个涉及到事务的数据库预提交(precommit)此操作，并反映是否可以提交。

事务协调器要求每个数据库提交数据，或者回滚数据。

### 优点 ###
- 尽量保证了数据的强一致，实现成本较低，在各大主流数据库都有自己实现，对于 MySQL 是从 5.5 开始支持。
### 缺点 ###
- 单点问题：事务管理器在整个流程中扮演的角色很关键，如果其宕机，比如在第一阶段已经完成，在第二阶段正准备提交的时候事务管理器宕机，资源管理器就会一直阻塞，导致数据库无法使用。
- 同步阻塞：在准备就绪之后，资源管理器中的资源一直处于阻塞，直到提交完成，释放资源。
- 数据不一致：两阶段提交协议虽然为分布式数据强一致性所设计，但仍然存在数据不一致性的可能。比如在第二阶段中，假设协调者发出了事务 Commit 的通知，但是因为网络问题该通知仅被一部分参与者所收到并执行了 Commit 操作，其余的参与者则因为没有收到通知一直处于阻塞状态，这时候就产生了数据的不一致性。总的来说，XA 协议比较简单，成本较低，但是其单点问题，以及不能支持高并发(由于同步阻塞)依然是其最大的弱点。

## 补偿事务（TCC） ##

对于 TCC 的解释：
- Try 阶段：尝试执行，完成所有业务检查（一致性），预留必需业务资源（准隔离性）。
- Confirm 阶段：确认真正执行业务，不作任何业务检查，只使用 Try 阶段预留的业务资源，Confirm 操作满足幂等性。要求具备幂等设计，Confirm 失败后需要进行重试。
- Cancel 阶段：取消执行，释放 Try 阶段预留的业务资源，Cancel 操作满足幂等性。Cancel 阶段的异常和 Confirm 阶段异常处理方案基本上一致。

举个简单的例子：如果你用 100 元买了一瓶水， 
Try 阶段：你需要向你的钱包检查是否够 100 元并锁住这 100 元，水也是一样的。如果有一个失败，则进行 Cancel(释放这 100 元和这一瓶水)，如果 Cancel 失败不论什么失败都进行重试 Cancel，所以需要保持幂等。如果都成功，则进行 Confirm，确认这 100 元被扣，和这一瓶水被卖，如果 Confirm 失败无论什么失败则重试(会依靠活动日志进行重试)。


## 本地消息表（异步确保） ##
此方案的核心是将需要分布式处理的任务通过消息日志的方式来异步执行。消息日志可以存储到本地文本、数据库或消息队列，再通过业务规则自动或人工发起重试。

人工重试更多的是应用于支付场景，通过对账系统对事后问题的处理。 


## MQ 事务消息 ##
在 RocketMQ 中实现了分布式事务，实际上是对本地消息表的一个封装，将本地消息表移动到了 MQ 内部。

基本流程如下：第一阶段 Prepared 消息，会拿到消息的地址。第二阶段执行本地事务。第三阶段通过第一阶段拿到的地址去访问消息，并修改状态。消息接受者就能使用这个消息。如果确认消息失败，在 RocketMQ Broker 中提供了定时扫描没有更新状态的消息。如果有消息没有得到确认，会向消息发送者发送消息，来判断是否提交，在 RocketMQ 中是以 Listener 的形式给发送者，用来处理。

## GTS--分布式事务解决方案 ##
GTS的核心优势

- 性能超强

GTS通过大量创新，解决了事务ACID特性与高性能、高可用、低侵入不可兼得的问题。单事务分支的平均响应时间在2ms左右，3台服务器组成的集群可以支撑3万TPS以上的分布式事务请求。

- 应用侵入性极低

GTS对业务低侵入，业务代码最少只需要添加一行注解（@TxcTransaction）声明事务即可。业务与事务分离，将微服务从事务中解放出来，微服务关注于业务本身，不再需要考虑反向接口、幂等、回滚策略等复杂问题，极大降低了微服务开发的难度与工作量。

- 完整解决方案

GTS支持多种主流的服务框架，包括EDAS，Dubbo，Spring Cloud等。
有些情况下，应用需要调用第三方系统的接口，而第三方系统没有接入GTS。此时需要用到GTS的MT模式。GTS的MT模式可以等价于TCC模式，用户可以根据自身业务需求自定义每个事务阶段的具体行为。MT模式提供了更多的灵活性，可能性，以达到特殊场景下的自定义优化及特殊功能的实现。

- 容错能力强

GTS解决了XA事务协调器单点问题，实现真正的高可用，可以保证各种异常情况下的严格数据一致。
