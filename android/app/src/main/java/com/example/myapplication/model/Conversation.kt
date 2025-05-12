package com.example.myapplication.model

import android.os.Parcel
import android.os.Parcelable
import java.util.Date
import java.util.UUID

data class Message(
    val text: String,
    val timestamp: Date = Date(),
    val isPartial: Boolean = false,
    val audioFilePath: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        Date(parcel.readLong()),
        parcel.readByte() != 0.toByte(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(text)
        parcel.writeLong(timestamp.time)
        parcel.writeByte(if (isPartial) 1 else 0)
        parcel.writeString(audioFilePath)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Message> {
        override fun createFromParcel(parcel: Parcel): Message {
            return Message(parcel)
        }

        override fun newArray(size: Int): Array<Message?> {
            return arrayOfNulls(size)
        }
    }
}

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Date = Date(),
    var lastUpdated: Date = Date(),
    val messages: MutableList<Message> = mutableListOf()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        Date(parcel.readLong()),
        Date(parcel.readLong()),
        mutableListOf<Message>().apply {
            parcel.createTypedArrayList(Message.CREATOR)?.let { addAll(it) }
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeLong(createdAt.time)
        parcel.writeLong(lastUpdated.time)
        parcel.writeTypedList(messages)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Conversation> {
        override fun createFromParcel(parcel: Parcel): Conversation {
            return Conversation(parcel)
        }

        override fun newArray(size: Int): Array<Conversation?> {
            return arrayOfNulls(size)
        }
    }
} 