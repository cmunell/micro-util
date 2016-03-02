package edu.cmu.ml.rtw.generic.opt.search;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.opt.search.Dimension.Type;

public class SearchGrid extends Search {
	public SearchGrid() {
		this(null);
	}
	
	public SearchGrid(Context context) {
		super(context);
	}
	
	@Override
	protected List<Position> getNextPositions() {
		return constructGrid(null, this.dimensions);
	}
	
	private List<Position> constructGrid(Position initialPosition, List<Dimension> dimensions) {
		List<Position> positions = new ArrayList<Position>();
		
		if (initialPosition != null) 
			positions.add(initialPosition);
		else
			positions.add(new Position(this.context));
		
		for (Dimension dimension : dimensions) {
			if (dimension.getType() != Type.ENUMERATED)
				return null;
			DimensionEnumerated dimensionEnumerated = (DimensionEnumerated)dimension;
			
			List<Position> newPositions = new ArrayList<Position>();
			
			for (Position position : positions) {
				for (int i = 0; i < dimensionEnumerated.getValues().size(); i++) {
					Position newPosition = position.clone();
					newPosition.setDimensionValue(dimensionEnumerated, dimensionEnumerated.getValues().get(i));
					List<Dimension> subDimensions = dimensionEnumerated.getSubDimensions(i);
					if (subDimensions == null)
						newPositions.add(newPosition);
					else 
						newPositions.addAll(constructGrid(newPosition, subDimensions));
				}
			}
			
			positions = newPositions;
		}
		
		return positions;
	}

	@Override
	public String getGenericName() {
		return "Grid";
	}

	@Override
	public Search makeInstance(Context context) {
		return new SearchGrid(context);
	}

}
