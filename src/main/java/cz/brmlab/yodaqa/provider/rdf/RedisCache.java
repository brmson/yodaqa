package cz.brmlab.yodaqa.provider.rdf;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import org.apache.jena.atlas.json.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Created by honza on 4.4.16.
 */
public class RedisCache {
	final Logger logger = LoggerFactory.getLogger(RedisCache.class);
	private static RedisCache instance = new RedisCache();
//	private Jedis jedis;
	private JedisPool pool = null;

	public RedisCache() {
//		JedisPoolConfig config = new JedisPoolConfig();
//		config.setMaxActive(1000);
		pool = new JedisPool("localhost");

//		jedis = new Jedis("localhost");
	}

	public static RedisCache getInstance() {
		return instance;
	}

	public String loadString(String key) {
		Jedis jedis = pool.getResource();
		String res = jedis.get(key);
		pool.returnResourceObject(jedis);
		return res;
	}

	public synchronized void storeString(String key, String value) {
		Jedis jedis = pool.getResource();
		jedis.set(key, value);
		pool.returnResourceObject(jedis);
	}

	public synchronized ResultSet loadResultSet(String key) {
		Jedis jedis = pool.getResource();
		logger.debug("KEY " + key);
		String val = jedis.get(key);
		logger.debug("VAL " + val);
		if (val == null) return null;
		InputStream is = new ByteArrayInputStream(val.getBytes(StandardCharsets.UTF_8));
		try {
			return ResultSetFactory.fromJSON(is);
		} catch (JsonParseException e) {
			return null;
		} finally {
			pool.returnResourceObject(jedis);
		}
	}

	public synchronized void storeResultSet(String key, ResultSet resultSet) {
		Jedis jedis = pool.getResource();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ResultSetFormatter.outputAsJSON(bos, resultSet);
		String val = new String(bos.toByteArray(), StandardCharsets.UTF_8);
		logger.debug("STORE KEY " + key);
		logger.debug("STORE VAL " + val);
		jedis.set(key, val);
		pool.returnResourceObject(jedis);
	}
}
