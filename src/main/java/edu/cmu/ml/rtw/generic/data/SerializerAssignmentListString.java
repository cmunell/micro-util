package edu.cmu.ml.rtw.generic.data;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.store.StoreReference;
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.NamedIterable;

public class SerializerAssignmentListString extends Serializer<NamedIterable<AssignmentList, Assignment>, String> {
	private List<Serializer.Index<NamedIterable<AssignmentList, Assignment>>> indices;
	
	public SerializerAssignmentListString(DataTools dataTools) {
		this.indices = new ArrayList<Serializer.Index<NamedIterable<AssignmentList, Assignment>>>();
		
		this.indices.add(new Serializer.Index<NamedIterable<AssignmentList, Assignment> >() {
			@Override
			public String getField() {
				return "name";
			}

			@Override
			public Object getValue(NamedIterable<AssignmentList, Assignment> item) {
				return item.getName();
			}
		});
	}

	@Override
	public String getName() {
		return "AssignmentListString";
	}

	@Override
	public String serialize(NamedIterable<AssignmentList, Assignment> item) {
		AssignmentList list = (AssignmentList)item.getIterable();
		list.add(Assignment.assignmentTyped(null, Context.ObjectType.VALUE.toString(), "serializationName", Obj.stringValue(item.getName())));
		return list.toString();
	}

	@Override
	public NamedIterable<AssignmentList, Assignment> deserialize(String object, StoreReference storeReference) {
		AssignmentList listPlusName = new AssignmentList();
		if (!listPlusName.fromString(object))
			return null;
		AssignmentList list = new AssignmentList();
		for (int i = 0; i < listPlusName.size() - 1; i++)
			list.add(listPlusName.get(i));
		String name = ((Obj.Value)listPlusName.get(listPlusName.size() - 1).getValue()).getStr();
		return new NamedIterable<AssignmentList, Assignment>(name, list);
	}

	@Override
	public String serializeToString(NamedIterable<AssignmentList, Assignment> item) {
		return serialize(item);
	}

	@Override
	public NamedIterable<AssignmentList, Assignment> deserializeFromString(String str, StoreReference storeReference) {
		return deserialize(str, storeReference);
	}

	@Override
	public List<Serializer.Index<NamedIterable<AssignmentList, Assignment> >> getIndices() {
		return this.indices;
	}

}
