# trips

#### 介绍
结合springAi的旅行计划AiAgent

#### 软件架构
springBoot+springAi+springCloudAlibaba

java 17，Spring Boot 3.3 + Spring Cloud 2023 + Spring Cloud Alibaba（Nacos 注册与配置）。 Apache Dubbo 3.x 做服务间 RPC；用户、偏好、文件、邮件、FAQ 等按域拆分为独立模块，API 层通过 @DubboReference 聚合调用。 Spring AI + Spring AI Alibaba（通义/DashScope） 构建对话与多模态能力；Qdrant 向量库支撑检索增强（RAG）。 Redis + Redisson（会话记忆、分布式锁等）；RocketMQ 消息；MyBatis-Plus + MySQL。 Sentinel 限流熔断（如 AI 接口）；Spring Boot Admin 客户端便于运维观测。


#### 安装教程

1.  如果需要使用分布式配置中心将每一个项目中的applicaiton.yml文件复制到配置中心即可

