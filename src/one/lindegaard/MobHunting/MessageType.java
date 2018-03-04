package one.lindegaard.MobHunting;

public enum MessageType {
	Chat("Chat"), ActionBar("ActionBar"), BossBar("BossBar");

	private final String name;

	private MessageType(String name) {
		this.name = name;
	}

	public boolean equalsName(String otherName) {
		return (otherName != null) && name.equals(otherName);
	}

	public String toString() {
		return name;
	}

	public MessageType valueOf(int id) {
		return MessageType.values()[id];
	}

	public String getName() {
		return name;
	}

}
