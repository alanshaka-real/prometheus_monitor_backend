package com.wenmin.prometheus.module.distribute.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * SSH 连接池，基于 commons-pool2 的 KeyedObjectPool。
 * 按 host:port:username 分组连接复用。
 */
@Slf4j
@Component
public class SshConnectionPool {

    private final KeyedObjectPool<String, SSHClient> pool;

    @Value("${distribute.ssh.strict-host-key-checking:false}")
    private boolean strictHostKeyChecking;

    public SshConnectionPool() {
        GenericKeyedObjectPoolConfig<SSHClient> config = new GenericKeyedObjectPoolConfig<>();
        config.setMaxTotal(20);
        config.setMaxTotalPerKey(3);
        config.setMinIdlePerKey(0);
        config.setMaxIdlePerKey(2);
        config.setMinEvictableIdleDuration(Duration.ofMinutes(5));
        config.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        config.setTestOnBorrow(true);
        config.setTestWhileIdle(true);
        config.setBlockWhenExhausted(true);
        config.setMaxWait(Duration.ofSeconds(10));

        this.pool = new GenericKeyedObjectPool<>(new SshClientFactory(), config);
    }

    /**
     * Generate pool key from connection parameters.
     */
    public static String poolKey(String host, int port, String username) {
        return host + ":" + port + ":" + username;
    }

    /**
     * Borrow an SSH client from the pool.
     */
    public SSHClient borrowClient(String host, int port, String username, String password) throws Exception {
        String key = poolKey(host, port, username);
        try {
            return pool.borrowObject(key);
        } catch (Exception e) {
            // If pool borrow fails, create a fresh connection
            log.debug("Pool borrow failed for {}, creating fresh connection: {}", key, e.getMessage());
            SSHClient ssh = createFreshClient(host, port, username, password);
            return ssh;
        }
    }

    /**
     * Return an SSH client to the pool.
     */
    public void returnClient(String host, int port, String username, SSHClient client) {
        String key = poolKey(host, port, username);
        try {
            pool.returnObject(key, client);
        } catch (Exception e) {
            log.debug("Failed to return SSH client to pool for {}: {}", key, e.getMessage());
            try {
                client.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Invalidate (destroy) a client that is no longer valid.
     */
    public void invalidateClient(String host, int port, String username, SSHClient client) {
        String key = poolKey(host, port, username);
        try {
            pool.invalidateObject(key, client);
        } catch (Exception e) {
            log.debug("Failed to invalidate SSH client for {}: {}", key, e.getMessage());
        }
    }

    private SSHClient createFreshClient(String host, int port, String username, String password) throws Exception {
        SSHClient ssh = new SSHClient();
        if (!strictHostKeyChecking) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
        } else {
            ssh.loadKnownHosts();
        }
        ssh.setConnectTimeout(10000);
        ssh.connect(host, port);
        ssh.authPassword(username, password);
        return ssh;
    }

    @PreDestroy
    public void destroy() {
        try {
            pool.close();
            log.info("SSH connection pool closed");
        } catch (Exception e) {
            log.error("Failed to close SSH connection pool", e);
        }
    }

    /**
     * Factory for creating SSH client objects in the pool.
     * Note: This factory creates unconnected clients; actual connection
     * happens outside the pool since passwords are not stored.
     */
    private static class SshClientFactory extends BaseKeyedPooledObjectFactory<String, SSHClient> {

        @Override
        public SSHClient create(String key) {
            return new SSHClient();
        }

        @Override
        public PooledObject<SSHClient> wrap(SSHClient client) {
            return new DefaultPooledObject<>(client);
        }

        @Override
        public boolean validateObject(String key, PooledObject<SSHClient> pooledObject) {
            SSHClient client = pooledObject.getObject();
            return client.isConnected() && client.isAuthenticated();
        }

        @Override
        public void destroyObject(String key, PooledObject<SSHClient> pooledObject) throws Exception {
            SSHClient client = pooledObject.getObject();
            if (client.isConnected()) {
                client.close();
            }
        }
    }
}
