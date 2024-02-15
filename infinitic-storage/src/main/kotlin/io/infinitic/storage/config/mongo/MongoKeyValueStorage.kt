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
package io.infinitic.storage.config.mongo

import com.mongodb.BasicDBObject
import com.mongodb.client.MongoClient
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.Projections
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import io.infinitic.storage.config.Mongo
import io.infinitic.storage.keyValue.KeyValueStorage
import org.bson.types.Binary
import org.jetbrains.annotations.TestOnly


private const val MONGO_COLLECTION = "keyValueStorage"
private val projectionValueField = Projections.fields(Projections.include("value"), Projections.excludeId())

class MongoKeyValueStorage(internal val client: MongoClient, internal val database: String) : KeyValueStorage {

  companion object {
    fun from(config: Mongo) = MongoKeyValueStorage(config.getPool(), config.database)
  }

  init {
    client.getDatabase(database).createCollection(MONGO_COLLECTION)
    client.getDatabase(database)
        .getCollection(MONGO_COLLECTION)
        .createIndex(Indexes.text("key"), IndexOptions().unique(true))
 }

  override suspend fun get(key: String): ByteArray? =
      client.getDatabase(database)
          .getCollection(MONGO_COLLECTION)
          .find(Filters.eq("key", key))
          .projection(projectionValueField)
          .first()?.let { (it.getValue("value") as Binary).data }

  override suspend fun put(key: String, value: ByteArray) {
    client.getDatabase(database)
        .getCollection(MONGO_COLLECTION)
        .updateOne(
            Filters.eq("key", key),
            Updates.set("value", value),
            UpdateOptions().upsert(true)
        )
  }

  override suspend fun del(key: String) {
    client.getDatabase(database).getCollection(MONGO_COLLECTION)
        .deleteOne(Filters.eq("key", key))
  }

  override fun close() {
    client.close()
  }

  @TestOnly
  override fun flush() {
    client.getDatabase(database).getCollection(MONGO_COLLECTION)
        .deleteMany(BasicDBObject())
  }
}
