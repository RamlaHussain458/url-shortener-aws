# 0007. Network design: public web subnet, private database subnets, no NAT gateway

**Status:** Accepted

**Context:** The application needs a web server that visitors can reach and a database that nobody on the internet can reach. NAT gateways cost money for every hour they exist, and the account runs on limited credits.

**Decision:** Create one VPC with two public subnets and two private subnets in two availability zones. The web server goes in a public subnet. The database goes in the private subnets, whose route table has no internet route. Security groups allow only HTTP and HTTPS to the web server, and MySQL to the database only from the web server's security group. There is no SSH rule. Shell access uses Systems Manager Session Manager.

**Consequences:**
- The database cannot be reached from the internet, even by mistake.
- There is no NAT gateway, so nothing in the private subnets can reach the internet. The database does not need to.
- The web server has a public address, so its security group is the main protection and must stay minimal.
- Two availability zones are needed anyway, because an RDS subnet group requires subnets in at least two zones.
- The network itself costs nothing. Cost only starts when servers, databases or public addresses are added.
