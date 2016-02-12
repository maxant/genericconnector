Run the image: (might need su -c "setenforce 0" beforehand; see http://stackoverflow.com/questions/24288616/permission-denied-on-accessing-host-directory-in-docker)

    docker run -it -p 9081:8080 maxant/genericconnector_acquirer
    docker run -it -p 9082:8080 maxant/genericconnector_bookingsystem
    docker run -it -p 9083:8080 maxant/genericconnector_letter

RUNNING ON MAXANT.CH: docker run -p.... &

WSDL is at:

- http://172.17.0.2:8080/genericconnector-demo-webservice-acquirer-2.1.1-SNAPSHOT/AcquirerWebService?wsdl
- http://localhost:9081/genericconnector-demo-webservice-acquirer-2.1.1-SNAPSHOT/AcquirerWebService?wsdl


- http://localhost:9082/genericconnector-demo-webservice-bookingsystem-2.1.1-SNAPSHOT/BookingSystemWebService?wsdl

- http://localhost:9083/genericconnector-demo-webservice-letter-2.1.1-SNAPSHOT/LetterWebService?wsdl


use `docker inspect <containername> | grep IPAddress` to see on which IP address its running


build acquirer image using 


    docker build -f Dockerfile_wildfly820 -t maxant/fedora23_jdk18_wildfly820 .
    
    docker build -f Dockerfile_acquirer -t maxant/genericconnector_acquirer .
    docker build -f Dockerfile_bookingsystem -t maxant/genericconnector_bookingsystem .
    docker build -f Dockerfile_letter -t maxant/genericconnector_letter .

    
    