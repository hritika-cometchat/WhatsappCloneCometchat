package com.example.testapplication.Model

class PinnedMessagesResponse {
    val data: Data? = null
}

 class Data {
     val pinnedMessages: List<PinnedMessage>?=null
 }

data class PinnedMessage(
    val id: String,
    val muid: String,
    val conversationId: String,
    val sender: String,
    val receiverType: String,
    val receiver: String,
    val category: String,
    val type: String,
    val data: MessageData,
    val sentAt: Long,
    val updatedAt: Long
)

data class MessageData(
    val entities: Entities,
    val metadata: Metadata,
    val resource: String,
    val text: String
)

data class Entities(
    val receiver: EntityWrapper,
    val sender: EntityWrapper
)

data class EntityWrapper(
    val entity: UserEntity,
    val entityType: String
)

data class UserEntity(
    val avatar: String,
    val conversationId: String? = null,
    val lastActiveAt: Long,
    val name: String,
    val role: String,
    val status: String,
    val uid: String
)

data class Metadata(
    val injected: InjectedMetadata
)

data class InjectedMetadata(
    val extensions: Extensions
)

data class Extensions(
    val humanModeration: HumanModeration
)

data class HumanModeration(
    val success: Boolean
)