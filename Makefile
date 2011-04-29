
run-client:
	mvn -e test-compile exec:java -Dexec.classpathScope="test" -Dexec.mainClass="com.cloudhopper.smpp.demo.ClientMain"

run-server:
	mvn -e test-compile exec:java -Dexec.classpathScope="test" -Dexec.mainClass="com.cloudhopper.smpp.demo.ServerMain"

run-simulator:
	mvn -e test-compile exec:java -Dexec.classpathScope="test" -Dexec.mainClass="com.cloudhopper.smpp.demo.SimulatorMain"

run-rebind:
	mvn -e test-compile exec:java -Dexec.classpathScope="test" -Dexec.mainClass="com.cloudhopper.smpp.demo.RebindMain"

run-parser:
	mvn -e test-compile exec:java -Dexec.classpathScope="test" -Dexec.mainClass="com.cloudhopper.smpp.demo.ParserMain"

run-dlr:
	mvn -e test-compile exec:java -Dexec.classpathScope="test" -Dexec.mainClass="com.cloudhopper.smpp.demo.DeliveryReceiptMain"