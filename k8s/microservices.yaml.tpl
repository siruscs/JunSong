{{- define "microservice.deployment" -}}
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: junsong-modules-{{ .Name }}
  namespace: junsong
spec:
  replicas: {{ .Replicas | default 2 }}
  selector:
    matchLabels:
      app: junsong-modules-{{ .Name }}
  template:
    metadata:
      labels:
        app: junsong-modules-{{ .Name }}
    spec:
      containers:
        - name: {{ .Name }}
          image: junsong-modules-{{ .Name }}:latest
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: {{ .Port }}
          envFrom:
            - configMapRef:
                name: junsong-config
            - secretRef:
                name: junsong-secrets
          resources:
            requests:
              memory: "256Mi"
              cpu: "200m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: {{ .Port }}
            initialDelaySeconds: 60
            periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: junsong-modules-{{ .Name }}
  namespace: junsong
spec:
  selector:
    app: junsong-modules-{{ .Name }}
  ports:
    - port: {{ .Port }}
      targetPort: {{ .Port }}
{{- end -}}

{{- $services := list
  (dict "Name" "system" "Port" 9201 "Replicas" 2)
  (dict "Name" "gen" "Port" 9202 "Replicas" 1)
  (dict "Name" "job" "Port" 9203 "Replicas" 1)
  (dict "Name" "file" "Port" 9300 "Replicas" 1)
  (dict "Name" "member" "Port" 9206 "Replicas" 2)
  (dict "Name" "finance" "Port" 9205 "Replicas" 2)
  (dict "Name" "workflow" "Port" 9207 "Replicas" 1)
  (dict "Name" "open" "Port" 9208 "Replicas" 2)
-}}

{{- range $services -}}
{{- include "microservice.deployment" . -}}
{{- end -}}
