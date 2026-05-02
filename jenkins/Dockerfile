FROM jenkins/jenkins:lts

# Bỏ qua wizard setup
ENV JAVA_OPTS -Djenkins.install.runSetupWizard=false

# Copy danh sách plugin và cài tự động
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli -f /usr/share/jenkins/ref/plugins.txt