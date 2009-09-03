package uk.me.parabola.splitter;

import java.util.List;

public interface SplittableArea {
	Area getBounds();

	List<Area> split(int maxNodes);
}
