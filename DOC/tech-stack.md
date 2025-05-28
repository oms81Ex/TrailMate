# PeakPal 기술 스택 명세서

## 1. 전체 아키텍처 개요

```
┌─────────────────────────────────────────────────┐
│                   Client Layer                  │
├─────────────────────────────────────────────────┤
│  📱 Mobile App        │  🌐 Web Dashboard      │
│  React Native         │  React.js              │
│  iOS / Android        │  Admin Panel           │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────┴───────────────────────────────┐
│                   API Gateway                   │
├─────────────────────────────────────────────────┤
│  🚪 AWS Application Load Balancer              │
│  🔐 AWS Cognito (Authentication)               │
│  📊 AWS CloudWatch (Monitoring)                │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────┴───────────────────────────────┐
│                Backend Services                 │
├─────────────────────────────────────────────────┤
│ 🎯 User Service    │ 📍 Tracking Service      │
│ 👥 Community       │ 🗺️ Map Service           │
│ 🆘 Safety Service  │ 📊 Analytics Service     │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────┴───────────────────────────────┐
│                 Data Layer                      │
├─────────────────────────────────────────────────┤
│ 🗄️ PostgreSQL     │ 📦 MongoDB               │
│ 🗂️ Redis Cache     │ 📁 AWS S3                │
│ 📈 InfluxDB        │ 🔍 Elasticsearch         │
└─────────────────────────────────────────────────┘
```

## 2. 프론트엔드 기술 스택

### 2.1 모바일 앱 (React Native)

#### 2.1.1 핵심 프레임워크
```json
{
  "framework": "React Native 0.72.x",
  "language": "TypeScript 5.0+",
  "stateManagement": "Redux Toolkit + RTK Query",
  "navigation": "React Navigation 6.x",
  "styling": "StyleSheet + react-native-reanimated"
}
```

#### 2.1.2 주요 라이브러리
```json
{
  "map": {
    "ios": "react-native-maps (Apple MapKit)",
    "android": "react-native-maps (Google Maps)",
    "offline": "react-native-offline-maps"
  },
  "location": {
    "gps": "@react-native-community/geolocation",
    "background": "react-native-background-job",
    "permissions": "react-native-permissions"
  },
  "storage": {
    "local": "@react-native-async-storage/async-storage",
    "secure": "react-native-keychain",
    "database": "react-native-sqlite-storage"
  },
  "ui": {
    "components": "react-native-elements",
    "icons": "react-native-vector-icons",
    "animations": "react-native-reanimated",
    "charts": "react-native-chart-kit"
  },
  "media": {
    "camera": "react-native-image-picker",
    "upload": "react-native-image-resizer"
  },
  "networking": {
    "http": "axios",
    "realtime": "socket.io-client",
    "offline": "react-native-netinfo"
  }
}
```

#### 2.1.3 개발 도구
```json
{
  "development": {
    "debugger": "Flipper",
    "testing": "Jest + React Native Testing Library",
    "e2e": "Detox",
    "linting": "ESLint + Prettier",
    "typecheck": "TypeScript"
  },
  "build": {
    "ios": "Xcode + CocoaPods",
    "android": "Android Studio + Gradle",
    "ci": "GitHub Actions",
    "distribution": "App Store Connect + Google Play Console"
  }
}
```

### 2.2 웹 대시보드 (React.js)

#### 2.2.1 핵심 기술
```json
{
  "framework": "React 18.x",
  "language": "TypeScript",
  "bundler": "Vite",
  "stateManagement": "Zustand",
  "routing": "React Router 6.x",
  "styling": "Tailwind CSS + Styled Components"
}
```

#### 2.2.2 UI 라이브러리
```json
{
  "components": "Ant Design",
  "charts": "Recharts + D3.js",
  "maps": "Leaflet + OpenStreetMap",
  "tables": "TanStack Table",
  "forms": "React Hook Form + Zod validation"
}
```

## 3. 백엔드 기술 스택

### 3.1 API 서버 (Node.js)

#### 3.1.1 핵심 프레임워크
```json
{
  "runtime": "Node.js 18.x LTS",
  "framework": "Express.js 4.x",
  "language": "TypeScript",
  "architecture": "Microservices",
  "messageQueue": "AWS SQS + Redis Pub/Sub"
}
```

#### 3.1.2 주요 라이브러리
```json
{
  "validation": "Joi / Zod",
  "authentication": "Passport.js + JWT",
  "authorization": "RBAC (Role-Based Access Control)",
  "orm": "Prisma (PostgreSQL) + Mongoose (MongoDB)",
  "caching": "Redis",
  "logging": "Winston + Morgan",
  "monitoring": "Prometheus + Grafana",
  "documentation": "Swagger/OpenAPI 3.0"
}
```

#### 3.1.3 마이크로서비스 구조
```typescript
// 서비스 별 포트 구성
const services = {
  userService: { port: 3001, database: 'PostgreSQL' },
  trackingService: { port: 3002, database: 'InfluxDB' },
  mapService: { port: 3003, database: 'MongoDB' },
  communityService: { port: 3004, database: 'PostgreSQL' },
  safetyService: { port: 3005, database: 'PostgreSQL' },
  analyticsService: { port: 3006, database: 'InfluxDB' },
  notificationService: { port: 3007, database: 'Redis' }
};
```

### 3.2 실시간 통신

#### 3.2.1 WebSocket 서버
```json
{
  "framework": "Socket.IO",
  "clustering": "Redis Adapter",
  "events": [
    "location_update",
    "sos_alert", 
    "group_tracking",
    "chat_message",
    "emergency_broadcast"
  ]
}
```

#### 3.2.2 Push 알림
```json
{
  "ios": "Apple Push Notification Service (APNs)",
  "android": "Firebase Cloud Messaging (FCM)",
  "web": "Web Push Protocol",
  "backend": "node-apn + firebase-admin"
}
```

## 4. 데이터베이스 설계

### 4.1 데이터베이스 분산 전략

```sql
-- 사용자 관련 데이터 (PostgreSQL)
Users, Profiles, Authentication, Settings

-- 커뮤니티 데이터 (PostgreSQL)  
Posts, Comments, Likes, Groups, Messages

-- 지도 및 등산로 데이터 (MongoDB)
Trails, POIs, MapTiles, GeospatialData

-- 시계열 추적 데이터 (InfluxDB)
LocationPoints, TrackingSessions, Metrics

-- 캐시 데이터 (Redis)
Sessions, TemporaryData, Queues

-- 파일 저장소 (AWS S3)
Photos, Videos, Documents, MapAssets
```

### 4.2 PostgreSQL 스키마 설계

```sql
-- 사용자 테이블
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    profile_image_url TEXT,
    hiking_level hiking_level_enum DEFAULT 'BEGINNER',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 등산 세션 테이블
CREATE TABLE tracking_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    trail_id UUID,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE,
    status session_status_enum DEFAULT 'ACTIVE',
    total_distance DECIMAL(10,2),
    total_time INTERVAL,
    elevation_gain DECIMAL(10,2),
    calories_burned INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 커뮤니티 게시물 테이블
CREATE TABLE community_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id UUID REFERENCES users(id),
    type post_type_enum NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    trail_id UUID,
    tracking_session_id UUID REFERENCES tracking_sessions(id),
    photos TEXT[],
    tags TEXT[],
    visibility visibility_enum DEFAULT 'PUBLIC',
    likes_count INTEGER DEFAULT 0,
    comments_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### 4.3 MongoDB 스키마 설계

```javascript
// 등산로 컬렉션
const trailSchema = {
  _id: ObjectId,
  name: String,
  description: String,
  difficulty: String, // 'EASY', 'MODERATE', 'HARD'
  location: {
    type: "Point",
    coordinates: [longitude, latitude]
  },
  path: {
    type: "LineString", 
    coordinates: [[longitude, latitude], ...]
  },
  distance: Number,
  elevationGain: Number,
  estimatedTime: Number,
  waypoints: [{
    name: String,
    type: String, // 'SUMMIT', 'REST', 'SHELTER', 'TOILET'
    location: {
      type: "Point",
      coordinates: [longitude, latitude]
    }
  }],
  rating: Number,
  reviewCount: Number,
  createdAt: Date,
  updatedAt: Date
};

// 지도 타일 컬렉션
const mapTileSchema = {
  _id: ObjectId,
  x: Number,
  y: Number, 
  z: Number,
  data: Buffer, // 타일 이미지 데이터
  format: String, // 'PNG', 'WEBP'
  createdAt: Date,
  expiresAt: Date
};
```

### 4.4 InfluxDB 스키마 설계

```javascript
// 위치 추적 포인트
const locationMeasurement = {
  measurement: 'location_points',
  tags: {
    user_id: 'uuid',
    session_id: 'uuid',
    device_type: 'ios|android'
  },
  fields: {
    latitude: 'float',
    longitude: 'float', 
    altitude: 'float',
    accuracy: 'float',
    speed: 'float',
    heading: 'float'
  },
  timestamp: 'nanosecond precision'
};

// 센서 데이터
const sensorMeasurement = {
  measurement: 'sensor_data',
  tags: {
    user_id: 'uuid',
    session_id: 'uuid',
    sensor_type: 'gps|accelerometer|gyroscope'
  },
  fields: {
    x: 'float',
    y: 'float', 
    z: 'float',
    accuracy: 'float'
  },
  timestamp: 'nanosecond precision'
};
```

## 5. 클라우드 인프라 (AWS)

### 5.1 컴퓨팅 서비스

```yaml
# ECS Fargate 클러스터 구성
services:
  api-gateway:
    image: nginx:alpine
    cpu: 256
    memory: 512
    replicas: 2
    
  user-service:
    image: peakpal/user-service:latest
    cpu: 512
    memory: 1024
    replicas: 3
    
  tracking-service:
    image: peakpal/tracking-service:latest  
    cpu: 1024
    memory: 2048
    replicas: 5 # 고부하 예상
    
  map-service:
    image: peakpal/map-service:latest
    cpu: 512
    memory: 1024
    replicas: 2
    
  community-service:
    image: peakpal/community-service:latest
    cpu: 256
    memory: 512  
    replicas: 2
```

### 5.2 데이터베이스 서비스

```yaml
databases:
  postgresql:
    service: "AWS RDS"
    engine: "PostgreSQL 14"
    instance: "db.r6g.large"
    storage: "500GB SSD"
    multiAZ: true
    backup: "7 days"
    
  mongodb:
    service: "AWS DocumentDB"
    instance: "db.r6g.large" 
    cluster: "3 nodes"
    storage: "100GB"
    
  influxdb:
    service: "AWS EC2"
    instance: "c6g.xlarge"
    storage: "1TB SSD"
    
  redis:
    service: "AWS ElastiCache"
    instance: "cache.r6g.large"
    cluster: true
```

### 5.3 스토리지 및 CDN

```yaml
storage:
  s3_buckets:
    - name: "peakpal-user-photos"
      access: "private"
      encryption: "AES-256"
      
    - name: "peakpal-map-tiles"  
      access: "public-read"
      cdn: "CloudFront"
      
    - name: "peakpal-app-assets"
      access: "public-read"
      cdn: "CloudFront"

cdn:
  cloudfront:
    distributions:
      - origin: "peakpal-map-tiles.s3.amazonaws.com"
        cache_policy: "1 day"
        
      - origin: "peakpal-app-assets.s3.amazonaws.com"  
        cache_policy: "7 days"
```

### 5.4 모니터링 및 로깅

```yaml
monitoring:
  cloudwatch:
    metrics:
      - "CPU Utilization"
      - "Memory Usage"  
      - "Request Count"
      - "Response Time"
      - "Error Rate"
      
    alarms:
      - metric: "ErrorRate > 5%"
        action: "SNS Alert"
        
      - metric: "ResponseTime > 2s"
        action: "Auto Scaling"

  logging:
    cloudwatch_logs:
      retention: "30 days"
      groups:
        - "/aws/ecs/api-gateway"
        - "/aws/ecs/user-service"  
        - "/aws/ecs/tracking-service"
        
  tracing:
    xray:
      enabled: true
      sampling_rate: 0.1
```

## 6. 외부 API 및 서비스

### 6.1 지도 및 위치 서비스

```json
{
  "google_maps": {
    "apis": [
      "Maps JavaScript API",
      "Directions API", 
      "Elevation API",
      "Geocoding API"
    ],
    "quota": "25,000 requests/day",
    "cost": "$200/month"
  },
  "apple_mapkit": {
    "apis": ["MapKit JS", "MapKit for iOS"],
    "quota": "25,000 requests/day", 
    "cost": "Free tier"
  },
  "openstreetmap": {
    "usage": "Backup mapping service",
    "cost": "Free"
  }
}
```

### 6.2 날씨 서비스

```json
{
  "korea_meteorological_administration": {
    "apis": [
      "단기예보",
      "중기예보", 
      "특보",
      "관측정보"
    ],
    "cost": "Free",
    "update_frequency": "3 hours"
  },
  "openweathermap": {
    "apis": ["Current Weather", "5-day Forecast"],
    "quota": "1,000 calls/day",
    "cost": "Free tier"
  }
}
```

### 6.3 인증 및 소셜 로그인

```json
{
  "oauth_providers": {
    "google": {
      "scopes": ["profile", "email"],
      "sdk": "google-auth-library"
    },
    "apple": {
      "scopes": ["name", "email"],
      "sdk": "apple-auth"  
    },
    "kakao": {
      "scopes": ["profile_nickname", "account_email"],
      "sdk": "kakao-login"
    }
  }
}
```

## 7. 보안 및 규정 준수

### 7.1 데이터 보안

```yaml
encryption:
  at_rest:
    database: "AES-256"
    storage: "AWS S3 Server-Side Encryption"
    
  in_transit:
    api: "TLS 1.3"
    database: "SSL/TLS"
    
  application:
    passwords: "bcrypt with salt"
    tokens: "JWT with RS256"
    sensitive_data: "AES-256-GCM"

secrets_management:
  service: "AWS Secrets Manager"
  rotation: "90 days"
  access: "IAM Role-based"
```

### 7.2 개인정보보호

```yaml
privacy_compliance:
  gdpr:
    data_portability: true
    right_to_deletion: true
    consent_management: true
    
  korea_pipa:
    location_data_protection: true
    user_consent_required: true
    data_retention_policy: "2 years"
    
  data_anonymization:
    location_data: "k-anonymity with k=5"
    user_analytics: "differential_privacy"
```

## 8. 개발 및 배포 파이프라인

### 8.1 CI/CD 파이프라인

```yaml
# .github/workflows/ci-cd.yml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
      - run: npm ci
      - run: npm run test
      - run: npm run lint
      - run: npm run type-check
      
  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - name: Build Docker Image
        run: docker build -t peakpal/api:${{ github.sha }} .
      - name: Push to ECR
        run: |
          aws ecr get-login-password | docker login --username AWS --password-stdin $ECR_URI
          docker push peakpal/api:${{ github.sha }}
          
  deploy:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - name: Deploy to ECS
        run: |
          aws ecs update-service --cluster peakpal --service api-service \
            --force-new-deployment
```

### 8.2 환경 구성

```yaml
environments:
  development:
    database: "Local PostgreSQL"
    redis: "Local Redis"
    s3: "LocalStack"
    monitoring: "Local logs"
    
  staging: 
    database: "AWS RDS (small instance)"
    redis: "AWS ElastiCache (small)"
    s3: "AWS S3 (staging bucket)"
    monitoring: "CloudWatch"
    
  production:
    database: "AWS RDS Multi-AZ"
    redis: "AWS ElastiCache Cluster"
    s3: "AWS S3 with CloudFront"
    monitoring: "CloudWatch + Grafana"
```

## 9. 성능 및 확장성

### 9.1 캐싱 전략

```yaml
caching_layers:
  application_cache:
    type: "In-memory (Node.js)"
    ttl: "5 minutes"
    size: "100MB"
    
  distributed_cache:
    type: "Redis Cluster" 
    ttl: "1 hour"
    size: "10GB"
    
  cdn_cache:
    type: "CloudFront"
    ttl: "24 hours"
    assets: ["images", "static_files"]
    
  database_cache:
    type: "Query result cache"
    ttl: "30 minutes"
    queries: ["trail_search", "user_profile"]
```

### 9.2 오토스케일링

```yaml
auto_scaling:
  ecs_services:
    target_cpu: 70%
    min_capacity: 2
    max_capacity: 10
    scale_out_cooldown: "5 minutes"
    scale_in_cooldown: "15 minutes"
    
  database:
    read_replicas: 
      min: 1
      max: 5
      cpu_threshold: 80%
      
  lambda_functions:
    concurrency: 1000
    timeout: "30 seconds"
    memory: "1024MB"
```

## 10. 비용 최적화

### 10.1 예상 월간 비용 (AWS)

```yaml
estimated_monthly_costs:
  compute:
    ecs_fargate: "$300"
    lambda: "$50"
    
  database:
    rds_postgresql: "$200"
    documentdb: "$150"
    elasticache: "$100"
    
  storage:
    s3: "$100"
    cloudfront: "$50"
    
  networking:
    load_balancer: "$25"
    data_transfer: "$75"
    
  monitoring:
    cloudwatch: "$30"
    
  total: "$1,080/month"
```

### 10.2 비용 절감 전략

```yaml
cost_optimization:
  reserved_instances:
    rds: "1 year reservation (-30%)"
    elasticache: "1 year reservation (-30%)"
    
  spot_instances:
    batch_processing: "Use Spot for analytics jobs"
    
  lifecycle_policies:
    s3_intelligent_tiering: "Automatic cost optimization"
    cloudwatch_logs: "30 day retention"
    
  resource_scheduling:
    development_env: "Auto shutdown after hours"
    staging_env: "Scale down during nights/weekends"
```

---

**문서 버전**: 1.0  
**작성일**: 2025-05-22  
**작성자**: 기술 아키텍트  
**검토자**: CTO, 시니어 개발자  
**승인일**: TBD