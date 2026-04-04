/**
 * 应用层（Application / Use-case）：编排领域服务、横切缓存与事务边界，供 {@code controller} 调用。
 * 典型模式：读路径可缓存、写路径通过基础设施层 AOP 失效相关缓存。
 */
package com.risk.backend.application;
