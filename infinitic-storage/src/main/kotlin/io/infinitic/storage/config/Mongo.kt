/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.storage.config

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.pojo.PojoCodecProvider
import java.util.concurrent.ConcurrentHashMap


data class Mongo(
  val connString: String = "mongodb://localhost",
  val database: String = "default",
) {
  companion object {
    val pools = ConcurrentHashMap<Mongo, MongoClient>()

    fun close() {
      pools.keys.forEach { it.close() }
    }
  }

  fun close() {
    pools[this]?.close()
    pools.remove(this)
  }

  fun getPool(): MongoClient =
      pools.computeIfAbsent(this) {
        val pojoCodecRegistry: CodecRegistry =
            CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
        val codecRegistry: CodecRegistry =
            CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry)
        MongoClients.create(
            MongoClientSettings.builder()
                .applyConnectionString(ConnectionString(connString))
                .codecRegistry(codecRegistry)
                .build(),
        )
      }
}
