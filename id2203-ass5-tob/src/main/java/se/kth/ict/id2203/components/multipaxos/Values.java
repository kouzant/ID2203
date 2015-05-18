package se.kth.ict.id2203.components.multipaxos;

public class Values<T extends Object> {
	private final T value;
	
	public Values(T value) {
		this.value = value;
	}
	
	public T getValue() {
		return value;
	}
	
	@Override
	public boolean equals(Object obj) {
		boolean equals = false;
		
		if (obj instanceof Values<?>) {
			equals = this.value.equals(((Values<?>) obj).getValue());
		}
		
		return equals;
	}
	
	@Override
	public int hashCode() {
		return (this.value.hashCode());
	}
}
