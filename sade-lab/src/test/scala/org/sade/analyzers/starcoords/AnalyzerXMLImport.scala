package org.sade.analyzers.starcoords

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.util.GregorianCalendar
import org.sade.starcoords.{Forward, AnalyzerXMLImport}

@RunWith(classOf[JUnitRunner])
class AnalyzerXMLImport extends Spec with MustMatchers {
  describe("ANX Import") {
    it("should parse simple case") {
      val xml = <ArrayOfSkyMapPoint xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
        <SkyMapPoint>
          <StandardDeviation>0.0010927760049637915</StandardDeviation>
          <Time>2010-03-27T10:25:48+03:00</Time>
          <Value>0.254788781626757</Value>
          <PointIndex>0</PointIndex>
          <PointCount>78</PointCount>
          <Direction>FORWARD</Direction>
          <CatchedPatternsPerPoint>2705</CatchedPatternsPerPoint>
          <DirIndex>0</DirIndex>
          <Frequency>200.33527046705689</Frequency>
        </SkyMapPoint>
        <SkyMapPoint>
          <StandardDeviation>0.000964597803390722</StandardDeviation>
          <Time>2010-03-11T22:20:57.46875+03:00</Time>
          <Value>0.25483535461422169</Value>
          <PointIndex>1</PointIndex>
          <PointCount>78</PointCount>
          <Direction>FORWARD</Direction>
          <CatchedPatternsPerPoint>2707</CatchedPatternsPerPoint>
          <DirIndex>0</DirIndex>
          <Frequency>200.5644427706161</Frequency>
        </SkyMapPoint>
      </ArrayOfSkyMapPoint>

      val result = AnalyzerXMLImport.parse(xml)
      result must have size (2)
      result(0).standardDeviation must be(0.0010927760049637915)
      result(0).time must be (new GregorianCalendar(2010, 2, 27, 10, 25, 48).getTime)
      result(0).pointCount must be (78)
      result(0).pointIndex must be (0)
      result(0).direction must be (Forward)
      result(0).dirIndex must be (0)
      result(0).rotationAngle must be (0)
    }
  }
}