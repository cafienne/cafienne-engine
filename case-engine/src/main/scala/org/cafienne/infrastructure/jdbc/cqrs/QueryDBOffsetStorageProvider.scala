package org.cafienne.infrastructure.jdbc.cqrs

import org.cafienne.infrastructure.cqrs.OffsetStorageProvider

class QueryDBOffsetStorageProvider extends OffsetStorageProvider {
  override def storage(name: String) = new QueryDBOffsetStorage(name)
}
