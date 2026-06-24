export interface RegionOption {
  value: string
  label: string
  children?: RegionOption[]
}

export const regionOptions: RegionOption[] = [
  {
    value: '440000',
    label: '广东省',
    children: [
      {
        value: '440300',
        label: '深圳市',
        children: [
          {
            value: '440305',
            label: '南山区',
            children: [
              { value: '440305007', label: '粤海街道' },
              { value: '440305005', label: '招商街道' },
            ],
          },
          {
            value: '440304',
            label: '福田区',
            children: [
              { value: '440304008', label: '福田街道' },
              { value: '440304006', label: '华强北街道' },
            ],
          },
        ],
      },
      {
        value: '440100',
        label: '广州市',
        children: [
          {
            value: '440106',
            label: '天河区',
            children: [
              { value: '440106006', label: '石牌街道' },
              { value: '440106014', label: '猎德街道' },
            ],
          },
        ],
      },
    ],
  },
  {
    value: '330000',
    label: '浙江省',
    children: [
      {
        value: '330100',
        label: '杭州市',
        children: [
          {
            value: '330106',
            label: '西湖区',
            children: [
              { value: '330106002', label: '北山街道' },
              { value: '330106013', label: '文新街道' },
            ],
          },
        ],
      },
    ],
  },
]
