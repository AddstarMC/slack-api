package au.com.addstar.slackapi.objects;

import java.lang.reflect.Type;
import java.util.List;

import au.com.addstar.slackapi.internal.Utilities;

import au.com.addstar.slackapi.objects.blocks.Block;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import lombok.*;

@Builder
@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class Message extends IdBaseObject
{
	@Setter
	private ObjectID userId;
	@Setter
	private String text;
	private ObjectID sourceId;
	private long timestamp;
	@Setter
	private String thread_ts;
	private String ts;
	@Setter
	private MessageType subtype;
	private ObjectID editUserId;
	private long editTimestamp;
	private boolean as_user = true;
	@Setter
	private List<Attachment> attachments;
	@Setter
	private List<Block> blocks;

	public Message(){
		this.subtype = MessageType.Normal;
		as_user = true;
	}
	
	public Message(String text, IdBaseObject channel)
	{
		this.sourceId = channel.getId();
		this.text = text;
		this.subtype = MessageType.Sent;
		as_user = true;
	}
	
	public static Object getGsonAdapter()
	{
		return new MessageJsonAdapter();
	}
	public void addBlock(Block block){
		blocks.add(block);
	}
	@Override
	public String toString()
	{
		return String.format("%s: '%s' from %s", subtype, text, userId);
	}
	
	private static class MessageJsonAdapter implements JsonDeserializer<Message>, JsonSerializer<Message>
	{
		@Override
		public Message deserialize( JsonElement element, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException
		{
			if (!(element instanceof JsonObject))
				throw new JsonParseException("Expected JSONObject as message root");
			
			JsonObject root = (JsonObject)element;
			
			Message message = new Message();
			if (root.has("user"))
				message.userId = new ObjectID(root.get("user").getAsString());
			
			message.text = Utilities.getAsString(root.get("text"));
			message.thread_ts = Utilities.getAsString(root.get("thread_ts"));
			message.ts = Utilities.getAsString(root.get("ts"));
			message.as_user = Utilities.getAsBoolean(root.get("as_user"),true);
			message.timestamp = Utilities.getAsTimestamp(root.get("ts"));
			if (root.has("channel"))
				message.sourceId = new ObjectID(root.get("channel").getAsString());
			
			if (root.has("edited"))
			{
				JsonObject edited = root.getAsJsonObject("edited");
				message.editUserId = new ObjectID(edited.get("user").getAsString());
				message.editTimestamp = Utilities.getAsTimestamp(edited.get("ts"));
			}
			
			message.subtype = MessageType.fromId(Utilities.getAsString(root.get("subtype")));
			
			if (root.has("attachments"))
			{
				message.attachments = Lists.newArrayList();
				JsonArray attachments = root.getAsJsonArray("attachments");
				for (JsonElement rawAttachment : attachments)
					message.attachments.add(context.<Attachment>deserialize(rawAttachment, Attachment.class));
			}
			if (root.has("blocks"))
			{
				message.blocks = Lists.newArrayList();
				JsonArray blocks = root.getAsJsonArray("blocks");
				for (JsonElement rawBlock : blocks)
					message.blocks.add(context.<Block>deserialize(rawBlock, Block.class));
			}
			return message;
		}

		@Override
		public JsonElement serialize( Message src, Type typeOfSrc, JsonSerializationContext context )
		{
			JsonObject object = new JsonObject();
			object.addProperty("type", "message");
			object.addProperty("channel", src.sourceId.toString());
			object.addProperty("text", src.text);
			object.addProperty("thread_ts",src.thread_ts);
			object.addProperty("as_user",src.as_user);

			if (src.attachments != null)
			{
				JsonArray attachments = new JsonArray();
				for (Attachment attachment : src.attachments)
					attachments.add(context.serialize(attachment));
				object.add("attachments", attachments);
			}
			if(src.blocks != null){
				JsonArray blocks = new JsonArray();
				for(Block block:src.blocks){
					blocks.add(context.serialize(block));
				}
				object.add("blocks",blocks);
			}
			return object;
		}
	}
	
	public enum MessageType
	{
		Normal(""),
		Sent(""),
		FromBot("bot_message"),
		FromMeCommand("me_message"),
		
		Edit("message_changed"),
		Delete("message_deleted"),
		
		ChannelJoin("channel_join"),
		ChannelLeave("channel_leave"),
		ChannelTopic("channel_topic"),
		ChannelPurpose("channel_purpose"),
		ChannelName("channel_name"),
		ChannelArchive("channel_archive"),
		ChannelUnarchive("channel_unarchive"),
		
		GroupJoin("group_join"),
		GroupLeave("group_leave"),
		GroupTopic("group_topic"),
		GroupPurpose("group_purpose"),
		GroupName("group_name"),
		GroupArchive("group_archive"),
		GroupUnarchive("group_unarchive"),
		
		FileShare("file_share"),
		FileComment("file_comment"),
		FileMention("file_mention");
		
		private final String id;
		
		private MessageType(String id)
		{
			this.id = id;
		}
		
		static MessageType fromId(String id)
		{
			if (id == null)
				return Normal;
			
			for (MessageType type : values())
			{
				if (type.id.equals(id))
					return type;
			}
			
			return Normal;
		}
	}
}
