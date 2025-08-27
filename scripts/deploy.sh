#!/bin/bash

# Deployment script for Prompt Generator SaaS
# Usage: ./scripts/deploy.sh [environment] [version]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values
ENVIRONMENT=${1:-production}
VERSION=${2:-latest}
NAMESPACE="prompt"

echo -e "${GREEN}🚀 Starting deployment to ${ENVIRONMENT} environment${NC}"
echo -e "${YELLOW}Version: ${VERSION}${NC}"
echo -e "${YELLOW}Namespace: ${NAMESPACE}${NC}"

# Function to check if kubectl is available
check_kubectl() {
    if ! command -v kubectl &> /dev/null; then
        echo -e "${RED}❌ kubectl is not installed or not in PATH${NC}"
        exit 1
    fi
}

# Function to check if namespace exists
check_namespace() {
    if ! kubectl get namespace $NAMESPACE &> /dev/null; then
        echo -e "${YELLOW}📦 Creating namespace ${NAMESPACE}${NC}"
        kubectl apply -f k8s/namespace.yaml
    fi
}

# Function to update secrets
update_secrets() {
    echo -e "${YELLOW}🔐 Updating secrets...${NC}"
    
    # Check if secrets file exists
    if [ ! -f "k8s/secret.yaml" ]; then
        echo -e "${RED}❌ k8s/secret.yaml not found${NC}"
        echo -e "${YELLOW}Please create the secrets file with your actual values${NC}"
        exit 1
    fi
    
    kubectl apply -f k8s/secret.yaml
}

# Function to apply configurations
apply_configs() {
    echo -e "${YELLOW}⚙️  Applying configurations...${NC}"
    kubectl apply -f k8s/configmap.yaml
}

# Function to update deployment
update_deployment() {
    echo -e "${YELLOW}📦 Updating deployment...${NC}"
    
    # Update the image tag in deployment
    if [ "$VERSION" != "latest" ]; then
        kubectl set image deployment/prompt-app prompt-app=<ECR_REGISTRY>/prompt-generator-saas:$VERSION -n $NAMESPACE
    fi
    
    kubectl apply -f k8s/deployment.yaml
}

# Function to apply services and ingress
apply_services() {
    echo -e "${YELLOW}🌐 Applying services and ingress...${NC}"
    kubectl apply -f k8s/service.yaml
    kubectl apply -f k8s/ingress.yaml
    kubectl apply -f k8s/hpa.yaml
}

# Function to wait for deployment
wait_for_deployment() {
    echo -e "${YELLOW}⏳ Waiting for deployment to be ready...${NC}"
    kubectl rollout status deployment/prompt-app -n $NAMESPACE --timeout=300s
}

# Function to check deployment health
check_health() {
    echo -e "${YELLOW}🏥 Checking application health...${NC}"
    
    # Wait a bit for pods to be ready
    sleep 10
    
    # Check if pods are running
    RUNNING_PODS=$(kubectl get pods -n $NAMESPACE -l app=prompt-app --field-selector=status.phase=Running --no-headers | wc -l)
    TOTAL_PODS=$(kubectl get pods -n $NAMESPACE -l app=prompt-app --no-headers | wc -l)
    
    echo -e "${YELLOW}📊 Pod Status: ${RUNNING_PODS}/${TOTAL_PODS} pods running${NC}"
    
    if [ $RUNNING_PODS -eq $TOTAL_PODS ] && [ $TOTAL_PODS -gt 0 ]; then
        echo -e "${GREEN}✅ All pods are running successfully!${NC}"
    else
        echo -e "${RED}❌ Some pods are not running properly${NC}"
        kubectl get pods -n $NAMESPACE -l app=prompt-app
        exit 1
    fi
}

# Function to show deployment info
show_info() {
    echo -e "${GREEN}🎉 Deployment completed successfully!${NC}"
    echo -e "${YELLOW}📋 Deployment Information:${NC}"
    echo -e "  Environment: ${ENVIRONMENT}"
    echo -e "  Version: ${VERSION}"
    echo -e "  Namespace: ${NAMESPACE}"
    echo -e ""
    echo -e "${YELLOW}🔍 Useful commands:${NC}"
    echo -e "  kubectl get pods -n ${NAMESPACE}"
    echo -e "  kubectl logs -f deployment/prompt-app -n ${NAMESPACE}"
    echo -e "  kubectl describe ingress prompt-ingress -n ${NAMESPACE}"
}

# Main deployment flow
main() {
    check_kubectl
    check_namespace
    update_secrets
    apply_configs
    update_deployment
    apply_services
    wait_for_deployment
    check_health
    show_info
}

# Run main function
main "$@"
