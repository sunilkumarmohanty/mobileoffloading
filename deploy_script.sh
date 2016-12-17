echo "\n ################ Generating image and pushing to google container registry ################ \n"

gcloud auth login
gcloud config set project mcc-2016-g07-p2
sudo docker build -t gcr.io/mcc-2016-g07-p2/backend:latest .
gcloud docker push gcr.io/mcc-2016-g07-p2/backend:latest

echo "\n ################ Creating cluster ################ \n"

gcloud container --project "mcc-2016-g07-p2" clusters create "mean-cluster" --zone "us-central1-f" --machine-type "n1-standard-2" --num-nodes "2" --network "default"

gcloud config set compute/zone us-central1-f
gcloud config set container/use_client_certificate True
gcloud container clusters get-credentials mean-cluster

echo "\n ################ Starting MongoDB replication ################ \n"

cd ./mongo-k8s-sidecar/example
make add-replica DISK_SIZE=200GB ZONE=us-central1-f
make add-replica DISK_SIZE=200GB ZONE=us-central1-f
make add-replica DISK_SIZE=200GB ZONE=us-central1-f

cd ../..
echo "\n ################ Creating the web controller and services ################ \n"

kubectl create -f web-controller.yml
kubectl create -f web-service.yml

echo "\n ################ Public IP Address - Run this till you see the Public IP ################ \n"

echo "Waiting for Server IP... "

for i in {1..100}
do
	gcloud compute forwarding-rules list | grep '104.154.187.219'
	if [ $? == 0 ]
	then
	    echo "Server IP = 104.154.187.219"
	    break
	fi
	sleep 1
done

#create user username/password
bash create_test_user.sh

#run android app build and deployment script
bash android_deploy.sh
