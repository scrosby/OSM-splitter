package uk.me.parabola.splitter;

public interface MapCollector extends MapProcessor {
	Area getExactArea();

	SplittableArea getRoundedArea(int resolution);
}
