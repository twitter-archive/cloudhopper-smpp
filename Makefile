
run-client:
	mvn test-compile exec:java -Dexec.classpathScope="test" -Dexec.mainClass="com.cloudhopper.smpp.demo.ClientMain"

run-server:
	mvn test-compile exec:java -Dexec.classpathScope="test" -Dexec.mainClass="com.cloudhopper.smpp.demo.ServerMain"

run-simulator:
	mvn test-compile exec:java -Dexec.classpathScope="test" -Dexec.mainClass="com.cloudhopper.smpp.demo.SimulatorMain"

run-rebind:
	mvn test-compile exec:java -Dexec.classpathScope="test" -Dexec.mainClass="com.cloudhopper.smpp.demo.RebindMain"

run-parser:
	mvn test-compile exec:java -Dexec.classpathScope="test" -Dexec.mainClass="com.cloudhopper.smpp.demo.ParserMain"

run-dlr:
	mvn test-compile exec:java -Dexec.classpathScope="test" -Dexec.mainClass="com.cloudhopper.smpp.demo.DeliveryReceiptMain"