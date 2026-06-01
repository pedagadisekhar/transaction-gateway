# Load Test Results

Tool: Apache JMeter 5.6.3

Target:
POST /v1/payments

Configuration:
- Threads: 250
- Ramp Up: 1 second
- Duration: 60 minutes

Results:
- Initial requests succeeded
- Under sustained load, HTTP 500 errors appeared
- Bottleneck likely database connection pool / Kafka consumer throughput

Artifacts:
- PCAP trace attached
- JMeter test plan attached
- Summary report attached